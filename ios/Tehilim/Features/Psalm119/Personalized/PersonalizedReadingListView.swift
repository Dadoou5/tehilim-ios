import SwiftUI

/// Affiche la séquence générée (ou rechargée depuis une prière sauvegardée).
///
/// Chaque ligne : `[index]  [lettre hébraïque]  [source]  [chevron]`
/// Tap → ouvre la section correspondante du Tehilim 119 avec contexte séquence.
struct PersonalizedReadingListView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var savedPrayers: SavedPrayerStore
    @Environment(\.dismiss) private var dismiss

    /// L'intent — toujours persisté en V1.10.5+ (auto-save iCloud).
    /// Le paramètre `isSaved` est conservé pour la rétrocompat du call site
    /// mais devrait toujours valoir `true` désormais.
    let intent: SavedPrayerIntent
    let isSaved: Bool

    var body: some View {
        List {
            // En-tête : sujet hébraïque + chip type
            Section {
                headerCard
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
            }

            // V1.10.7 — Bloc Commémoration affiché en haut quand l'utilisateur
            // a renseigné une date du décès. Donne la date de la prochaine
            // azcara calculée par MemorialCalculator.
            if let death = intent.civilDateOfDeath,
               let next = MemorialCalculator.nextYahrzeit(deathCivil: death) {
                Section {
                    HStack(spacing: 14) {
                        Image(systemName: "flame.fill")
                            .font(.title3)
                            .foregroundStyle(Color.accentMain)
                            .accessibilityHidden(true)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Prochaine azcara")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .textCase(.uppercase)
                            // Astérisque rappelle que le Hebrew day commence
                            // au coucher du soleil de la veille civile.
                            Text("\(next.formatted(date: .complete, time: .omitted))*")
                                .font(.headline)
                        }
                        Spacer()
                    }
                    .padding(.vertical, 4)
                    .accessibilityElement(children: .combine)
                } footer: {
                    Text("* commence la veille au soir")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            // Liste des lettres numérotée
            Section {
                ForEach(Array(intent.generatedLetters.enumerated()), id: \.element.id) { idx, item in
                    NavigationLink(destination: sectionDestination(for: item, at: idx)) {
                        letterRow(item: item, displayIndex: idx + 1)
                    }
                }
            } header: {
                HStack {
                    Text("Séquence de lecture")
                    Spacer()
                    Text("\(intent.generatedLetters.count) lettres")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.secondary)
                }
            } footer: {
                Text("« נשמה » a été ajouté automatiquement à la fin de la séquence.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            // Bouton « Reprendre la lecture » si saved + lastReadIndex
            if isSaved, let lastIndex = currentLastIndex {
                Section {
                    NavigationLink(destination: sectionDestination(
                        for: intent.generatedLetters[lastIndex],
                        at: lastIndex
                    )) {
                        HStack(spacing: 12) {
                            Image(systemName: "arrow.uturn.right.circle.fill")
                                .foregroundStyle(Color.accentMain)
                            VStack(alignment: .leading) {
                                Text("Reprendre la lecture")
                                    .font(.headline)
                                Text("À la lettre \(lastIndex + 1) sur \(intent.generatedLetters.count)")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .appBackground()
        .navigationTitle(intent.title)
        .navigationBarTitleDisplayMode(.inline)
        // V1.10.5 : le bouton « Sauvegarder » a été retiré — la sauvegarde
        // est désormais automatique au tap sur « Générer » dans le formulaire,
        // et synchronisée via iCloud entre les appareils.
    }

    // MARK: - Composants

    @ViewBuilder
    private var headerCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                Label("Lelouy Nichmat", systemImage: "flame.fill")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Color.accentMain, in: Capsule())
                Spacer()
            }
            Text(intent.hebrewSubject)
                .font(.title2.weight(.semibold))
                .environment(\.layoutDirection, .rightToLeft)
                .frame(maxWidth: .infinity, alignment: .trailing)
            Text("נשמה")
                .font(.title3)
                .foregroundStyle(Color.accentMain)
                .frame(maxWidth: .infinity, alignment: .trailing)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.bgSurface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(Color.dividerToken.opacity(0.4), lineWidth: 0.5)
        )
        .padding(.horizontal, 16)
        .padding(.bottom, 8)
    }

    @ViewBuilder
    private func letterRow(item: ReadingLetterItem, displayIndex: Int) -> some View {
        HStack(spacing: 12) {
            // Index
            Text("\(displayIndex)")
                .font(.callout.weight(.semibold))
                .foregroundStyle(.secondary)
                .frame(width: 32, alignment: .leading)

            // Lettre hébraïque
            Text(item.character)
                .font(.system(size: 30, weight: .regular, design: .serif))
                .frame(width: 40)
                .foregroundStyle(.primary)

            // Source
            VStack(alignment: .leading, spacing: 2) {
                Text("Source")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
                    .textCase(.uppercase)
                Text(item.source.label)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color.accentMain)
                    .environment(
                        \.layoutDirection,
                        item.source == .neshama ? .rightToLeft : .leftToRight
                    )
            }
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            "Lettre \(displayIndex) sur \(intent.generatedLetters.count), " +
            "\(item.character), source \(item.source.label)"
        )
        .accessibilityHint("Ouvre la section correspondante du Tehilim 119")
    }

    // MARK: - Navigation vers la section

    @ViewBuilder
    private func sectionDestination(for item: ReadingLetterItem, at position: Int) -> some View {
        if let section = container.psalm119Repository.section(forLetter: item.psalmLetterKey) {
            Psalm119SectionView(
                index: section.index,
                sequenceContext: PsalmSequenceContext(
                    items: intent.generatedLetters,
                    currentPosition: position,
                    savedIntentId: isSaved ? intent.id : nil
                )
            )
        } else {
            EmptyStateView(
                symbol: "exclamationmark.triangle",
                title: "Section introuvable",
                message: "La lettre \(item.character) ne correspond à aucune section du Tehilim 119."
            )
        }
    }

    // MARK: - Helpers

    /// Récupère le `lastReadIndex` à jour depuis le store (l'intent local peut être périmé).
    private var currentLastIndex: Int? {
        guard isSaved,
              let stored = savedPrayers.intents.first(where: { $0.id == intent.id }),
              let idx = stored.lastReadIndex,
              intent.generatedLetters.indices.contains(idx)
        else { return nil }
        return idx
    }
}

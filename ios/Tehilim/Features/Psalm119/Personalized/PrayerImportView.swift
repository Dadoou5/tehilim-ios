import SwiftUI

/// Aperçu + confirmation d'import d'une prière reçue par lien partagé
/// (`tehilim://prayer?...`). Présentée en `.sheet` depuis `RootTabView`.
///
/// Le destinataire voit le sujet hébraïque, le type, et la prochaine azcara
/// si une date du décès est transmise — puis confirme l'import (ou annule).
/// Évite les imports accidentels et rend explicite ce qui sera ajouté.
struct PrayerImportView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var savedPrayers: SavedPrayerStore
    @Environment(\.dismiss) private var dismiss

    let payload: PrayerShareLink.Payload

    /// Pile interne à la feuille : permet d'ouvrir la prière (nouvelle ou
    /// existante) directement dans le modal après confirmation, sans toucher
    /// à la navigation par onglets de l'app.
    @State private var path = NavigationPath()

    /// La prière déjà sauvegardée correspondant au payload (mêmes prénom +
    /// lien + mère), ou nil si c'est un nouvel import.
    private var existing: SavedPrayerIntent? {
        savedPrayers.findExisting(
            relativeFirstName: payload.relativeFirstName,
            relationType: payload.relationType,
            motherFirstName: payload.motherFirstName
        )
    }

    private var alreadyExists: Bool { existing != nil }

    var body: some View {
        NavigationStack(path: $path) {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 10) {
                        Label(payload.prayerType.saveActionTitle, systemImage: "flame.fill")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(Color.accentMain, in: Capsule())
                        Text(payload.hebrewSubject)
                            .font(.title2.weight(.semibold))
                            .environment(\.layoutDirection, .rightToLeft)
                            .frame(maxWidth: .infinity, alignment: .trailing)
                    }
                    .padding(.vertical, 6)
                } header: {
                    Text("Prière reçue")
                }

                if let death = payload.civilDateOfDeath,
                   let next = MemorialCalculator.nextYahrzeit(deathCivil: death) {
                    Section {
                        HStack(spacing: 12) {
                            Image(systemName: "flame.fill")
                                .foregroundStyle(Color.accentMain)
                                .accessibilityHidden(true)
                            Text("Prochaine azcara")
                            Spacer()
                            Text("\(next.formatted(date: .abbreviated, time: .omitted))*")
                                .fontWeight(.medium)
                                .foregroundStyle(Color.accentMain)
                        }
                    } footer: {
                        Text("* commence la veille au soir")
                    }
                }

                if alreadyExists {
                    Section {
                        Label(
                            "Cette prière est déjà dans Mes prières.",
                            systemImage: "checkmark.circle"
                        )
                        .font(.callout)
                        .foregroundStyle(.secondary)
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Importer")
            .navigationBarTitleDisplayMode(.inline)
            // Ouverture de la prière (nouvelle ou existante) au sein du modal.
            .navigationDestination(for: SavedPrayerIntent.self) { intent in
                PersonalizedReadingListView(intent: intent, isSaved: true)
            }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(alreadyExists ? "Ouvrir" : "Importer") { confirm() }
                        .fontWeight(.semibold)
                }
            }
        }
    }

    private func confirm() {
        // addOrFindExisting : ajoute la nouvelle prière, ou retourne le
        // doublon existant (pas de duplication). Dans les deux cas on ouvre
        // ensuite la prière dans la pile interne de la feuille — ce qui rend
        // le bouton « Ouvrir » fonctionnel y compris pour un doublon.
        let candidate = PrayerShareLink.makeIntent(from: payload)
        let saved = savedPrayers.addOrFindExisting(candidate)
        path.append(saved)
    }
}

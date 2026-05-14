import SwiftUI

struct Psalm119HomeView: View {
    @EnvironmentObject private var container: AppContainer
    @EnvironmentObject private var savedPrayers: SavedPrayerStore
    @Environment(\.horizontalSizeClass) private var hSize

    @State private var showingForm: Bool = false

    private var isRegular: Bool { hSize == .regular }

    private var columns: [GridItem] {
        Array(
            repeating: GridItem(.flexible(), spacing: isRegular ? 16 : 12),
            count: AdaptiveLayout.psalm119ColumnCount(for: hSize)
        )
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: isRegular ? 20 : 16) {
                if isRegular {
                    headerCard
                }

                // V1.10.0 — Entry points lecture personnalisée
                personalizedSection

                // Grille alphabet classique
                LazyVGrid(columns: columns, spacing: isRegular ? 16 : 12) {
                    ForEach(container.psalm119Repository.sections) { section in
                        NavigationLink(destination: Psalm119SectionView(index: section.index)) {
                            HebrewLetterTile(
                                letter: section.letter,
                                index: section.index,
                                name: section.name,
                                verseStart: section.verseStart,
                                verseEnd: section.verseEnd
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(.horizontal, AdaptiveLayout.horizontalPadding(for: hSize))
            .padding(.vertical, isRegular ? 24 : 16)
        }
        .background(Color.bgPrimary)
        .navigationTitle("119 - AlphaBeta")
        .sheet(isPresented: $showingForm) {
            PersonalizedReadingFormView()
        }
    }

    // MARK: - Header iPad

    @ViewBuilder
    private var headerCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Tehilim 119")
                .font(.title2.weight(.semibold))
                .foregroundStyle(.primary)
            Text("Les 22 lettres de l'alphabet hébreu, huit versets chacune.")
                .font(.callout)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(Color.bgSurface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(Color.dividerToken.opacity(0.4), lineWidth: 0.5)
        )
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }

    // MARK: - Section lecture personnalisée

    @ViewBuilder
    private var personalizedSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Lelouy Nichmat")
                .font(.headline)
                .accessibilityAddTraits(.isHeader)

            LazyVGrid(
                columns: Array(
                    repeating: GridItem(.flexible(), spacing: 12),
                    count: isRegular ? 2 : 1
                ),
                spacing: 12
            ) {
                // Carte : nouvelle lecture
                Button { showingForm = true } label: {
                    actionCard(
                        symbol: "flame.fill",
                        title: "Nouvelle lecture",
                        subtitle: "Élévation de l'âme — prénom + mère",
                        accent: true
                    )
                }
                .buttonStyle(.plain)

                // Carte : prières sauvegardées
                NavigationLink(destination: SavedPrayersListView()) {
                    actionCard(
                        symbol: "tray.full",
                        title: "Mes prières",
                        subtitle: savedCountLabel,
                        accent: false
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var savedCountLabel: String {
        let count = savedPrayers.intents.count
        switch count {
        case 0: return "Aucun sauvegardé"
        case 1: return "1 sauvegardé"
        default: return "\(count) sauvegardés"
        }
    }

    @ViewBuilder
    private func actionCard(symbol: String, title: LocalizedStringKey, subtitle: String, accent: Bool) -> some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Color.accentMain.opacity(accent ? 0.18 : 0.10))
                    .frame(width: 44, height: 44)
                Image(systemName: symbol)
                    .font(.title3.weight(.medium))
                    .foregroundStyle(Color.accentMain)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.headline)
                    .foregroundStyle(.primary)
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.tertiary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.bgSurface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(Color.dividerToken.opacity(0.4), lineWidth: 0.5)
        )
    }
}

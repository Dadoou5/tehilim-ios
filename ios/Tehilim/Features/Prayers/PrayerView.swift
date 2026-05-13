import SwiftUI

/// Affiche une prière (avant / après lecture) — réutilise VerseRowView pour respecter
/// le mode (hébreu/phonétique), les tailles et la préférence de traduction.
struct PrayerView: View {
    let prayer: Prayer

    @EnvironmentObject private var container: AppContainer
    @StateObject private var prefs = Preferences()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0) {
                    header
                    Divider().padding(.horizontal, 16)
                    ForEach(verses) { v in
                        VerseRowView(
                            verse: v,
                            showTranslation: prefs.translationFR,
                            textMode: prefs.textMode,
                            textSizeHebrew: prefs.textSizeHebrew,
                            textSizeFR: prefs.textSizeFR,
                            numberStyle: prefs.verseNumberStyle,
                            translationLang: prefs.appLanguage.translation
                        )
                        .padding(.horizontal, 16)
                        Divider().padding(.horizontal, 16).opacity(0.3)
                    }
                }
                .padding(.bottom, 24)
            }
            .background(Color.bgPrimary)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    Text(prayer.kind.titleFR)
                        .font(.headline)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Fermer") { dismiss() }
                }
            }
        }
    }

    @ViewBuilder
    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(prayer.kind.titleFR)
                .font(.title3.weight(.semibold))
                .accessibilityAddTraits(.isHeader)
            Text(prayer.kind.subtitleFR)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.top, 16)
        .padding(.bottom, 12)
    }

    /// Résolution des références → versets concrets (depuis le corpus chargé).
    private var verses: [Verse] {
        prayer.verseRefs.compactMap { ref in
            guard let p = container.psalmRepository.psalm(id: ref.psalmId) else { return nil }
            return p.verses.first { $0.number == ref.verseNumber }
        }
    }
}

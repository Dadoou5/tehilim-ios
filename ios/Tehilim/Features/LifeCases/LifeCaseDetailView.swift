import SwiftUI

struct LifeCaseDetailView: View {
    @EnvironmentObject private var container: AppContainer
    @Environment(\.horizontalSizeClass) private var hSize
    let caseId: String

    @State private var presentedPrayer: Prayer.Kind? = nil

    /// Sur iPad : grille de 3 colonnes pour les Tehilim. Sur iPhone : liste verticale.
    private var psalmGridColumns: [GridItem] {
        let count = hSize == .regular ? 3 : 1
        return Array(repeating: GridItem(.flexible(), spacing: 12), count: count)
    }

    var body: some View {
        Group {
            if let c = container.lifeCaseRepository.find(id: caseId) {
                content(for: c)
            } else {
                EmptyStateView(
                    symbol: "exclamationmark.triangle",
                    title: "Catégorie introuvable",
                    message: nil
                )
            }
        }
    }

    @ViewBuilder
    private func content(for c: LifeCase) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                headerCard(for: c)
                prayerButton(.before)
                psalmsList(for: c)
                prayerButton(.after)
                disclaimerFooter
            }
            .padding(.horizontal, AdaptiveLayout.horizontalPadding(for: hSize))
            .padding(.vertical, 16)
            .readingWidth()
        }
        .background(Color.bgPrimary)
        .navigationTitle(c.localizedTitle)
        .navigationBarTitleDisplayMode(.large)
        .sheet(item: $presentedPrayer) { kind in
            PrayerView(prayer: Prayer.of(kind))
        }
    }

    @ViewBuilder
    private func headerCard(for c: LifeCase) -> some View {
        HStack(alignment: .top, spacing: 16) {
            ZStack {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(Color.accentMain.opacity(0.12))
                    .frame(width: 56, height: 56)
                Image(systemName: c.symbol)
                    .font(.title.weight(.medium))
                    .foregroundStyle(Color.accentMain)
            }
            .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 6) {
                Text(c.localizedNote)
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
                Text(psalmCountLabel(for: c))
                    .font(.caption.weight(.medium))
                    .foregroundStyle(.tertiary)
            }
            Spacer(minLength: 0)
        }
        .padding(16)
        .background(Color.bgSurface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(Color.dividerToken.opacity(0.4), lineWidth: 0.5)
        )
    }

    @ViewBuilder
    private func prayerButton(_ kind: Prayer.Kind) -> some View {
        Button {
            presentedPrayer = kind
        } label: {
            HStack(spacing: 12) {
                Image(systemName: kind.symbol)
                    .font(.title3)
                    .foregroundStyle(Color.accentMain)
                    .frame(width: 28)
                VStack(alignment: .leading, spacing: 2) {
                    Text(kind.titleFR)
                        .font(.headline)
                        .foregroundStyle(.primary)
                    Text(kind.subtitleFR)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.bgSurface)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(Color.dividerToken.opacity(0.4), lineWidth: 0.5)
            )
        }
        .buttonStyle(.plain)
        .accessibilityHint("Affiche la prière à dire \(kind == .before ? "avant" : "après") la lecture")
    }

    @ViewBuilder
    private func psalmsList(for c: LifeCase) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Tehilim à lire")
                .font(.title3.weight(.semibold))
                .accessibilityAddTraits(.isHeader)

            LazyVGrid(columns: psalmGridColumns, spacing: 10) {
                ForEach(c.psalms, id: \.self) { id in
                    if let p = container.psalmRepository.psalm(id: id) {
                        NavigationLink(destination: PsalmDetailView(psalmId: id, siblings: c.psalms)) {
                            psalmTile(p)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func psalmTile(_ p: Psalm) -> some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text("Tehilim \(p.id)")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                Text(p.hebrewNumber)
                    .font(.caption)
                    .foregroundStyle(Color.accentMain)
                    .environment(\.layoutDirection, .rightToLeft)
            }
            Spacer(minLength: 0)
            Image(systemName: "chevron.right")
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.tertiary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.bgSurface)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .stroke(Color.dividerToken.opacity(0.4), lineWidth: 0.5)
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Tehilim \(p.id)")
        .accessibilityHint("Ouvre la lecture")
        .accessibilityAddTraits(.isButton)
    }

    @ViewBuilder
    private var disclaimerFooter: some View {
        Text("Tradition. Ne remplace pas un avis professionnel.")
            .font(.footnote)
            .foregroundStyle(.tertiary)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .padding(.top, 8)
            .padding(.bottom, 16)
    }

    // V1.10.7 — localisé via L() (Text(stringVar) ne passe pas par
    // LocalizedStringKey, d'où le besoin du helper).
    private func psalmCountLabel(for c: LifeCase) -> String {
        let n = c.psalms.count
        return n == 1
            ? L("1 Tehilim recommandé")
            : String(format: L("%lld Tehilim recommandés"), n)
    }
}

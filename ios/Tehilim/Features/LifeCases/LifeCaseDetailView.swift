import SwiftUI

struct LifeCaseDetailView: View {
    @EnvironmentObject private var container: AppContainer
    let caseId: String

    @State private var presentedPrayer: Prayer.Kind? = nil

    var body: some View {
        Group {
            if let c = container.lifeCaseRepository.find(id: caseId) {
                List {
                    Section {
                        Text(c.localizedNote)
                            .font(.callout)
                            .foregroundStyle(.secondary)
                            .padding(.vertical, 4)
                    }

                    Section {
                        Button {
                            presentedPrayer = .before
                        } label: {
                            Label(Prayer.Kind.before.titleFR, systemImage: Prayer.Kind.before.symbol)
                        }
                    }

                    Section("Tehilim") {
                        ForEach(c.psalms, id: \.self) { id in
                            if let p = container.psalmRepository.psalm(id: id) {
                                NavigationLink(destination: PsalmDetailView(psalmId: id, siblings: c.psalms)) {
                                    Text("Tehilim \(p.id) · \(p.hebrewNumber)")
                                }
                            }
                        }
                    }

                    Section {
                        Button {
                            presentedPrayer = .after
                        } label: {
                            Label(Prayer.Kind.after.titleFR, systemImage: Prayer.Kind.after.symbol)
                        }
                    }

                    Section {
                        Text("Tradition. Ne remplace pas un avis professionnel.")
                            .font(.footnote).foregroundStyle(.tertiary)
                    }
                }
                .listStyle(.insetGrouped)
                .appBackground()
                .navigationTitle(c.localizedTitle)
                .sheet(item: $presentedPrayer) { kind in
                    PrayerView(prayer: Prayer.of(kind))
                }
            } else {
                EmptyStateView(symbol: "exclamationmark.triangle", title: "Catégorie introuvable", message: nil)
            }
        }
    }
}

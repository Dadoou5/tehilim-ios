import SwiftUI

private struct ArchivePsalmNav: Identifiable, Hashable { let id: Int }

/// Lecture **hors-ligne** d'une chaîne distribuée, à partir de l'instantané local
/// (aucun appel réseau). Permet de continuer à lire ses Tehilim en mode avion —
/// le texte des Tehilim est déjà 100 % embarqué dans l'app.
struct ChainArchiveReaderView: View {
    @EnvironmentObject private var container: AppContainer
    @Environment(\.horizontalSizeClass) private var hSize
    let snapshot: ChainArchiveSnapshot
    @State private var reading: ArchivePsalmNav?

    private var myIds: [Int] { (snapshot.myPsalmIds ?? []).sorted() }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                header

                if !myIds.isEmpty {
                    Label("Mes Tehilim", systemImage: "book")
                        .font(.headline)
                    LazyVGrid(columns: AdaptiveLayout.adaptiveColumns(for: hSize, compactMin: 58, regularMin: 80, spacing: 8), spacing: 8) {
                        ForEach(myIds, id: \.self) { id in
                            Button { reading = ArchivePsalmNav(id: id) } label: { cell(id) }
                                .buttonStyle(.plain)
                        }
                    }
                } else {
                    Text("Aucun Tehilim ne t'a été attribué dans cette chaîne.")
                        .font(.callout).foregroundStyle(.secondary)
                }

                breakdown
            }
            .padding(.horizontal, AdaptiveLayout.horizontalPadding(for: hSize))
            .padding(.vertical, 16)
            .readingWidth(maxWidth: AdaptiveLayout.dashboardMaxWidth)
        }
        .background(Color.bgPrimary)
        .navigationTitle("Chaîne de Tehilim")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                ShareLink(item: reportText) { Image(systemName: "square.and.arrow.up") }
            }
        }
        .navigationDestination(item: $reading) { nav in
            PsalmDetailView(psalmId: nav.id, siblings: myIds)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Label(LocalizedStringKey(snapshot.intention.titleKey), systemImage: snapshot.intention.symbol)
                .font(.caption.weight(.semibold))
                .foregroundStyle(snapshot.intention.tint)
            Text(snapshot.subjectLine).font(.title3.weight(.semibold))
            (Text("Lecture jusqu'au") + Text(" " + snapshot.readingDeadline.formatted(date: .abbreviated, time: .omitted)))
                .font(.caption).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.bgSurface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func cell(_ id: Int) -> some View {
        let minutes = container.psalmRepository.psalm(id: id)?.estimatedReadingMinutes ?? 1
        return VStack(spacing: 2) {
            Text("\(id)").font(.callout.weight(.semibold))
            Text("~\(minutes) min").font(.caption2)
        }
        .frame(maxWidth: .infinity, minHeight: 46)
        .foregroundStyle(Color.white)
        .background(Color.accentMain, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
    }

    @ViewBuilder
    private var breakdown: some View {
        if !snapshot.assignments.isEmpty {
            VStack(alignment: .leading, spacing: 6) {
                Label("Répartition", systemImage: "list.bullet")
                    .font(.headline)
                ForEach(byName, id: \.name) { row in
                    HStack(alignment: .top) {
                        Text(row.name).font(.caption).frame(width: 110, alignment: .leading)
                        Text(TehilimChain.compressRanges(row.ids, separator: AppLocale.code == "en" ? "to" : "à"))
                            .font(.caption).foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
            .padding(16)
            .background(Color.bgSurface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
    }

    private var byName: [(name: String, ids: [Int])] {
        var map: [String: [Int]] = [:]
        for (k, name) in snapshot.assignments { map[name, default: []].append(Int(k) ?? 0) }
        return map.map { (name: $0.key, ids: $0.value.sorted()) }.sorted { $0.name < $1.name }
    }

    private var reportText: String {
        let en = AppLocale.code == "en"
        var lines = [(en ? "Tehilim chain — " : "Chaîne de Tehilim — ") + snapshot.subjectLine, ""]
        for row in byName {
            lines.append("• \(row.name) : \(TehilimChain.compressRanges(row.ids, separator: en ? "to" : "à"))")
        }
        return lines.joined(separator: "\n")
    }
}

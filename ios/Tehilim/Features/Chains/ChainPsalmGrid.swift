import SwiftUI

/// Filtre d'affichage de la grille.
enum ChainGridFilter: String, CaseIterable, Identifiable {
    case all, free, mine
    var id: String { rawValue }
    var titleKey: String {
        switch self {
        case .all:  return "Tous"
        case .free: return "Libres"
        case .mine: return "Les miens"
        }
    }
}

/// Grille des 150 Tehilim d'une chaîne. Chaque pavé : libre (sélectionnable),
/// « à moi » (déselectionnable), ou pris par un autre (verrouillé, nom affiché).
struct ChainPsalmGrid: View {
    let assignments: [Int: ChainAssignment]
    let currentUid: String?
    /// Sélection encore ouverte (sinon lecture seule → tap = lire).
    let selectionOpen: Bool
    /// Action de sélection/déselection (phase ouverte).
    let onToggle: (Int) -> Void
    /// Action de lecture (phase verrouillée) — ouvre le Tehilim.
    let onRead: (Int) -> Void
    /// Temps de lecture estimé (min) par numéro de Tehilim — affiché sur les
    /// cases libres (remplacé par le nom dès qu'un Tehilim est réservé).
    let minutesFor: (Int) -> Int
    /// Filtre d'affichage (tous / libres / les miens).
    var filter: ChainGridFilter = .all

    @Environment(\.horizontalSizeClass) private var hSize

    private var columns: [GridItem] {
        AdaptiveLayout.adaptiveColumns(for: hSize, compactMin: 58, regularMin: 80, spacing: 8)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            ForEach(TehilimBook.allCases) { book in
                let ids = visibleIds(in: book)
                if !ids.isEmpty {
                    Text(LocalizedStringKey(book.titleKey))
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.secondary)
                        .padding(.leading, 2)
                    LazyVGrid(columns: columns, spacing: 8) {
                        ForEach(ids, id: \.self) { id in cell(for: id) }
                    }
                }
            }
            if TehilimBook.allCases.allSatisfy({ visibleIds(in: $0).isEmpty }) {
                Text(LocalizedStringKey(emptyFilterText))
                    .font(.callout).foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 24)
            }
        }
    }

    private func visibleIds(in book: TehilimBook) -> [Int] {
        book.range.filter { id in
            switch filter {
            case .all:  return true
            case .free: return assignments[id] == nil
            case .mine: return assignments[id]?.uid == currentUid
            }
        }
    }

    private var emptyFilterText: String {
        switch filter {
        case .mine: return "Tu n'as pas encore réservé de Tehilim."
        case .free: return "Tous les Tehilim sont pris 🎉"
        case .all:  return ""
        }
    }

    @ViewBuilder
    private func cell(for id: Int) -> some View {
        let assignment = assignments[id]
        let isMine = assignment != nil && assignment?.uid == currentUid
        let isTakenByOther = assignment != nil && !isMine

        Button {
            if selectionOpen {
                if isTakenByOther { return }       // verrouillé
                onToggle(id)
            } else {
                onRead(id)
            }
        } label: {
            VStack(spacing: 2) {
                Text("\(id)")
                    .font(.callout.weight(.semibold))
                    .foregroundStyle(isMine ? Color.white : .primary)
                if let assignment {
                    // Pris → le nom remplace le temps de lecture.
                    Text(assignment.name)
                        .font(.system(size: 9))
                        .lineLimit(1)
                        .truncationMode(.tail)
                        .foregroundStyle(isMine ? Color.white.opacity(0.9) : .secondary)
                } else {
                    // Libre → temps de lecture estimé.
                    Text("~\(minutesFor(id)) min")
                        .font(.system(size: 9))
                        .foregroundStyle(.tertiary)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 44)
            .background(background(isMine: isMine, isTaken: isTakenByOther))
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .stroke(Color.dividerToken.opacity(0.4), lineWidth: 0.5)
            )
            .opacity(selectionOpen && isTakenByOther ? 0.55 : 1)
        }
        .buttonStyle(.plain)
        .disabled(selectionOpen && isTakenByOther)
        .accessibilityLabel(accessibility(id: id, assignment: assignment, isMine: isMine))
    }

    private func background(isMine: Bool, isTaken: Bool) -> Color {
        if isMine { return Color.accentMain }
        if isTaken { return Color.bgSurface.opacity(0.6) }
        return Color.bgSurface
    }

    private func accessibility(id: Int, assignment: ChainAssignment?, isMine: Bool) -> String {
        if isMine { return "Tehilim \(id), réservé par toi" }
        if let a = assignment { return "Tehilim \(id), pris par \(a.name)" }
        return "Tehilim \(id), libre"
    }
}

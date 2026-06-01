import SwiftUI

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

    @Environment(\.horizontalSizeClass) private var hSize

    private var columns: [GridItem] {
        AdaptiveLayout.adaptiveColumns(for: hSize, compactMin: 58, regularMin: 80, spacing: 8)
    }

    var body: some View {
        LazyVGrid(columns: columns, spacing: 8) {
            ForEach(1...TehilimChain.totalPsalms, id: \.self) { id in
                cell(for: id)
            }
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
                    Text(assignment.name)
                        .font(.system(size: 9))
                        .lineLimit(1)
                        .truncationMode(.tail)
                        .foregroundStyle(isMine ? Color.white.opacity(0.9) : .secondary)
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

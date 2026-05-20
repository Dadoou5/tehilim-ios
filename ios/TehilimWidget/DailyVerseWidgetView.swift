import SwiftUI
import WidgetKit

struct DailyVerseWidgetView: View {
    @Environment(\.widgetFamily) private var family
    @Environment(\.colorScheme) private var scheme
    let entry: DailyVerseEntry

    private var accent: Color {
        scheme == .dark ? WidgetPalette.accentDark : WidgetPalette.accentLight
    }

    private var modeLabel: LocalizedStringKey {
        switch entry.mode {
        case .monthly: return "Cycle mensuel"
        case .weekly:  return "Jour de la semaine"
        case .custom:  return "Personnalisé"
        }
    }

    var body: some View {
        switch family {
        case .systemSmall:  smallView
        case .systemMedium: mediumView
        case .systemLarge:  largeView
        default:            mediumView
        }
    }

    // MARK: - Small (158×158)

    private var smallView: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Header
            HStack(spacing: 4) {
                Image(systemName: "sun.max.fill")
                    .font(.caption2)
                    .foregroundStyle(accent)
                Text("Aujourd'hui")
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .textCase(.uppercase)
            }
            Spacer(minLength: 4)

            // Liste compacte des numéros — max 6 sur le widget carré
            if entry.todayPsalms.isEmpty {
                Text("—")
                    .font(.title2.weight(.bold))
                    .foregroundStyle(.secondary)
            } else {
                let visible = Array(entry.todayPsalms.prefix(6))
                LazyVGrid(columns: [.init(.flexible()), .init(.flexible())], spacing: 4) {
                    ForEach(visible, id: \.id) { p in
                        psalmChip(p, compact: true)
                    }
                }
                if entry.todayPsalms.count > visible.count {
                    Text("+ \(entry.todayPsalms.count - visible.count) autres")
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                        .padding(.top, 2)
                }
            }
            Spacer(minLength: 0)

            // Footer
            Text("\(entry.todayPsalms.count) Tehilim")
                .font(.caption2)
                .foregroundStyle(.tertiary)
        }
    }

    // MARK: - Medium (338×158)

    private var mediumView: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                HStack(spacing: 4) {
                    Image(systemName: "sun.max.fill")
                        .font(.caption2)
                        .foregroundStyle(accent)
                    Text("Tehilim du jour")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Text(entry.hebrewDate)
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
                    .environment(\.layoutDirection, .rightToLeft)
            }

            // Grille de chips
            if entry.todayPsalms.isEmpty {
                Text("Aucun Tehilim défini")
                    .foregroundStyle(.secondary)
            } else {
                let visible = Array(entry.todayPsalms.prefix(8))
                LazyVGrid(columns: [.init(.flexible()), .init(.flexible()), .init(.flexible()), .init(.flexible())], spacing: 6) {
                    ForEach(visible, id: \.id) { p in
                        psalmChip(p, compact: false)
                    }
                }
                if entry.todayPsalms.count > visible.count {
                    Text("+ \(entry.todayPsalms.count - visible.count) autres Tehilim")
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                }
            }

            Spacer(minLength: 0)

            HStack {
                Text(modeLabel)
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
                Spacer()
                Text("\(entry.todayPsalms.count) Tehilim")
                    .font(.caption2.weight(.medium))
                    .foregroundStyle(accent)
            }
        }
    }

    // MARK: - Large (338×354)

    private var largeView: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 4) {
                    Image(systemName: "sun.max.fill")
                        .font(.caption)
                        .foregroundStyle(accent)
                    Text("Tehilim du jour")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.secondary)
                        .textCase(.uppercase)
                    Spacer()
                    Text(modeLabel)
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                }
                Text(entry.hebrewDate)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(.primary)
                    .environment(\.layoutDirection, .rightToLeft)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }

            Divider().opacity(0.3)

            // Liste des psaumes
            if !entry.todayPsalms.isEmpty {
                let columnsCount = entry.todayPsalms.count > 8 ? 4 : 3
                let columns = Array(
                    repeating: GridItem(.flexible(), spacing: 6),
                    count: columnsCount
                )
                LazyVGrid(columns: columns, spacing: 6) {
                    ForEach(entry.todayPsalms.prefix(12), id: \.id) { p in
                        psalmChip(p, compact: false)
                    }
                }
                if entry.todayPsalms.count > 12 {
                    Text("+ \(entry.todayPsalms.count - 12) autres Tehilim")
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                }
            }

            Spacer(minLength: 0)

            // Verset accent
            if !entry.firstVerseHebrew.isEmpty {
                VStack(alignment: .trailing, spacing: 4) {
                    Text(entry.firstVerseHebrew)
                        .font(.system(size: 14))
                        .multilineTextAlignment(.trailing)
                        .environment(\.layoutDirection, .rightToLeft)
                        .lineSpacing(2)
                        .lineLimit(2)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                    if let fr = entry.firstVerseFR {
                        Text(fr)
                            .font(.system(size: 11, design: .serif).italic())
                            .foregroundStyle(.secondary)
                            .lineLimit(2)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                .padding(.top, 4)
            }
        }
    }

    // MARK: - Helper : un chip "23 · כג"

    private func psalmChip(_ p: DailyVerseEntry.PsalmRef, compact: Bool) -> some View {
        VStack(spacing: 1) {
            Text("\(p.id)")
                .font(compact ? .caption.weight(.semibold) : .subheadline.weight(.semibold))
                .foregroundStyle(.primary)
            Text(p.hebrewNumber)
                .font(compact ? .caption2 : .caption)
                .foregroundStyle(accent)
                .environment(\.layoutDirection, .rightToLeft)
        }
        .frame(maxWidth: .infinity, minHeight: compact ? 28 : 36)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(.ultraThinMaterial)
        )
    }
}

#Preview(as: .systemSmall) {
    DailyVerseWidget()
} timeline: {
    DailyVerseEntry.placeholder
}

#Preview(as: .systemMedium) {
    DailyVerseWidget()
} timeline: {
    DailyVerseEntry.placeholder
}

#Preview(as: .systemLarge) {
    DailyVerseWidget()
} timeline: {
    DailyVerseEntry.placeholder
}

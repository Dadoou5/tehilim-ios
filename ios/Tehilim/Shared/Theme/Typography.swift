import SwiftUI

extension Font {
    /// Hébreu : sans-serif (design `.default`) — meilleure lisibilité avec le nikud
    /// que la version sérif. Base 22 pt à l'échelle `m`.
    static func hebrewBody(_ size: TextSize) -> Font {
        let base: CGFloat = 22
        return .system(size: base * size.scale, weight: .regular, design: .default)
    }

    static func hebrewTitle(_ size: TextSize = .l) -> Font {
        let base: CGFloat = 28
        return .system(size: base * size.scale, weight: .semibold, design: .default)
    }

    /// Français : sérif (New York via `.serif`) — confort de lecture longue.
    /// Base 17 pt à l'échelle `m`.
    static func frBody(_ size: TextSize) -> Font {
        let base: CGFloat = 17
        return .system(size: base * size.scale, weight: .regular, design: .serif)
    }

    static func verseNumber(_ size: TextSize) -> Font {
        let base: CGFloat = 13
        return .system(size: max(11, base * size.scale), weight: .medium, design: .monospaced)
    }
}

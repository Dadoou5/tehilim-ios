import SwiftUI

extension Font {
    /// Hébreu : Ezra SIL SR (SIL OFL, embarquée).
    /// Police hébraïque biblique avec support complet des téamim (cantillation) et nikud.
    static func hebrewBody(_ size: TextSize) -> Font {
        let base: CGFloat = 22
        return .custom("EzraSILSR", size: base * size.scale)
    }

    static func hebrewTitle(_ size: TextSize = .l) -> Font {
        let base: CGFloat = 28
        return .custom("EzraSILSR", size: base * size.scale).weight(.semibold)
    }

    /// Français / anglais : sérif (New York via `.serif`) — confort de lecture longue.
    static func frBody(_ size: TextSize) -> Font {
        let base: CGFloat = 17
        return .system(size: base * size.scale, weight: .regular, design: .serif)
    }

    static func verseNumber(_ size: TextSize) -> Font {
        let base: CGFloat = 13
        return .system(size: max(11, base * size.scale), weight: .medium, design: .monospaced)
    }
}

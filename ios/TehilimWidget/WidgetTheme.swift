import SwiftUI

/// Palette du widget — valeurs alignées sur l'Asset catalog principal mais
/// hardcodées car le widget ne peut pas accéder aux Color names de l'app.
enum WidgetPalette {
    // Light mode
    static let bgLight   = Color(red: 0.945, green: 0.969, blue: 0.984)  // bgPrimary clair
    static let bgLight2  = Color(red: 0.780, green: 0.878, blue: 0.961)  // bgElevated clair
    static let surfaceLight = Color(red: 1.0, green: 1.0, blue: 1.0).opacity(0.55)

    // Dark mode
    static let bgDark    = Color(red: 0.047, green: 0.067, blue: 0.106)  // bgPrimary sombre
    static let bgDark2   = Color(red: 0.145, green: 0.176, blue: 0.251)  // bgElevated sombre
    static let surfaceDark = Color(red: 0.102, green: 0.133, blue: 0.208).opacity(0.55)

    // Accent
    static let accentLight = Color(red: 0.118, green: 0.251, blue: 0.686)  // accentMain clair
    static let accentDark  = Color(red: 0.576, green: 0.773, blue: 0.992)  // accentMain sombre
}

/// Fond dégradé bleu d'eau du widget.
struct WidgetBackground: View {
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        LinearGradient(
            colors: scheme == .dark
                ? [WidgetPalette.bgDark, WidgetPalette.bgDark2]
                : [WidgetPalette.bgLight, WidgetPalette.bgLight2],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

/// Couleur d'accent adaptative.
struct WidgetAccentColor {
    @Environment(\.colorScheme) var scheme
    var value: Color { scheme == .dark ? WidgetPalette.accentDark : WidgetPalette.accentLight }
}

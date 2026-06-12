import Foundation

/// Helper global pour résoudre une chaîne localisée **en passant explicitement
/// par `Bundle.main.localizedString(...)`** (donc par le swizzle).
///
/// Indispensable parce que `String(localized:)` (iOS 15+) passe par
/// CoreFoundation internalement — court-circuite `object_setClass`. Les enums
/// `AppTheme`, `TextSize`, `TextMode`, `DailyMode`, etc. doivent utiliser ce
/// helper au lieu de `String(localized:)` pour que leurs labels suivent la
/// bascule de langue à chaud.
@inline(__always)
func L(_ key: String) -> String {
    Bundle.main.localizedString(forKey: key, value: nil, table: nil)
}

/// Swizzle de `Bundle.main` pour que `Text("…")` SwiftUI résolve les chaînes
/// contre la `.lproj` correspondant à la préférence utilisateur
/// `pref.app.language` — indépendamment de `AppleLanguages` global.
///
/// **V2.1.b** — installé au démarrage par `TehilimApp.init()`. Combiné à
/// `RootTabView().id(prefs.appLanguage)`, ça permet une bascule à chaud
/// sans relancer l'app.
///
/// Tous les call sites SwiftUI (`Text("Réglages")`, `NavigationLink("…")`,
/// `Label("…", systemImage:)`, etc.) passent par
/// `Bundle.main.localizedString(forKey:value:table:)`. Comme on remplace la
/// classe de `Bundle.main` par `LocalizedBundle`, cet override intercepte
/// tous les lookups sans aucune modification des call sites.
enum LocalizedBundleInstaller {
    private static var installed = false

    static func installOnce() {
        guard !installed else { return }
        installed = true
        object_setClass(Bundle.main, LocalizedBundle.self)
    }
}

/// Bundle.main re-classé en `LocalizedBundle` à l'install. L'override délègue
/// à la sub-bundle de la langue choisie (`en.lproj`/`fr.lproj`) si la pref
/// utilisateur est `.fr` ou `.en` ; sinon (`.system`) on retombe sur le
/// comportement standard d'iOS (qui résout via `AppleLanguages` ou la langue
/// système).
private final class LocalizedBundle: Bundle, @unchecked Sendable {
    override func localizedString(forKey key: String, value: String?, table tableName: String?) -> String {
        if let lang = userLanguageOverride(),
           let path = Bundle.main.path(forResource: lang, ofType: "lproj"),
           let bundle = Bundle(path: path) {
            return bundle.localizedString(forKey: key, value: value, table: tableName)
        }
        return super.localizedString(forKey: key, value: value, table: tableName)
    }

    /// Lecture directe de la préférence — pas de dépendance sur la classe
    /// `Preferences` (lazy-initialized, consommerait ce bundle).
    ///
    /// **AppGroup d'abord** pour que le widget voie la même préférence que
    /// l'app (les deux processus partagent ce store). Fallback sur
    /// `UserDefaults.standard` pour les cas où AppGroup tombe en .standard
    /// (free provisioning, capability absente) — voir `AppGroup.userDefaults`.
    ///
    /// Retourne `nil` pour `.system` (le comportement standard reprend).
    private func userLanguageOverride() -> String? {
        let raw = AppGroup.userDefaults.string(forKey: AppGroup.Keys.appLanguage)
            ?? UserDefaults.standard.string(forKey: "pref.app.language")
            ?? "system"
        switch raw {
        case "fr": return "fr"
        case "en": return "en"
        case "he": return "he"
        default:
            // `.system` : suit l'appareil si fr ou hébreu (iOS peut rapporter
            // l'hébreu sous l'ancien code « iw ») ; sinon anglais.
            let pref = Locale.preferredLanguages.first ?? "en"
            let code = String(pref.split(separator: "-").first ?? "en")
            if code == "he" || code == "iw" { return "he" }
            return code == "fr" ? "fr" : "en"
        }
    }
}

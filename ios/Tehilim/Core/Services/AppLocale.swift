import Foundation

/// Source unique de vérité pour la langue **effective** du contenu et des dates.
///
/// Pourquoi pas `Locale.current` ? Parce que c'est un **instantané figé au
/// lancement** : après une bascule de langue à chaud (ex. Anglais → Système),
/// `Locale.current` reste sur l'ancienne langue jusqu'au prochain démarrage,
/// alors que `Locale.preferredLanguages` (et donc le swizzle qui pilote l'UI)
/// reflète déjà la nouvelle. Résultat : UI en français mais dates / Cas de la
/// vie en anglais. On lit donc la même source que l'UI : la préférence in-app,
/// et pour `.system` la 1ʳᵉ langue préférée de l'appareil (live).
enum AppLocale {

    /// Code langue effectif : "fr" ou "en".
    static var code: String {
        let raw = AppGroup.userDefaults.string(forKey: AppGroup.Keys.appLanguage)
            ?? UserDefaults.standard.string(forKey: "pref.app.language")
            ?? "system"
        switch raw {
        case "fr": return "fr"
        case "en": return "en"
        default:
            // `.system` : suit l'appareil si français, sinon anglais (l'app
            // n'est traduite qu'en fr/en). Aligné sur le swizzle UI.
            let pref = Locale.preferredLanguages.first ?? "en"
            return pref.hasPrefix("fr") ? "fr" : "en"
        }
    }

    /// `Locale` correspondant (pour DateFormatter / FormatStyle).
    static var locale: Locale {
        Locale(identifier: code == "fr" ? "fr_FR" : "en_US")
    }
}

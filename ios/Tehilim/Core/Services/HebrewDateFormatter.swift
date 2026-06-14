import Foundation

enum HebrewDateFormatter {
    struct DisplayDate {
        let dayOfWeek: String          // "Lundi" / "Monday"
        let transliterated: String     // "7 Iyar 5786"
        let hebrew: String             // "ז׳ באייר ה׳תשפ״ו"
    }

    /// Résout la locale du jour de la semaine depuis `pref.app.language` —
    /// indispensable pour suivre la bascule de langue à chaud (V2.1.b). Avant,
    /// `Locale(identifier: "fr_FR")` était hardcodé, donc « Lundi » restait
    /// FR même quand l'app passait en EN.
    private static func dayOfWeekLocale() -> Locale {
        let raw = AppGroup.userDefaults.string(forKey: AppGroup.Keys.appLanguage)
            ?? UserDefaults.standard.string(forKey: "pref.app.language")
            ?? "system"
        switch raw {
        case "fr": return Locale(identifier: "fr_FR")
        case "en": return Locale(identifier: "en_US")
        case "he": return Locale(identifier: "he_IL")
        default:
            // .system : on lit `Locale.preferredLanguages` (live) et NON
            // `Locale.current` (instantané figé qui reste sur l'ancienne langue
            // après une bascule à chaud → jour de la semaine en anglais alors
            // que le système est en français). Hébreu / français si l'appareil
            // est dans ces langues, anglais sinon.
            let pref = Locale.preferredLanguages.first ?? "fr"
            if pref.hasPrefix("he") || pref.hasPrefix("iw") { return Locale(identifier: "he_IL") }
            return pref.hasPrefix("fr") ? Locale(identifier: "fr_FR") : Locale(identifier: "en_US")
        }
    }

    static func formatted(_ date: Date = Date()) -> DisplayDate {
        let dowFmt = DateFormatter()
        dowFmt.locale = dayOfWeekLocale()
        dowFmt.dateFormat = "EEEE"
        let day = dowFmt.string(from: date).capitalized(with: dowFmt.locale)

        let latinFmt = DateFormatter()
        latinFmt.calendar = Calendar(identifier: .hebrew)
        latinFmt.locale = Locale(identifier: "en_US_POSIX")
        latinFmt.dateFormat = "d MMMM yyyy"
        let latin = latinFmt.string(from: date)

        let heFmt = DateFormatter()
        heFmt.calendar = Calendar(identifier: .hebrew)
        heFmt.locale = Locale(identifier: "he_IL")
        heFmt.dateStyle = .long
        let hebrew = heFmt.string(from: date)

        return DisplayDate(dayOfWeek: day, transliterated: latin, hebrew: hebrew)
    }
}

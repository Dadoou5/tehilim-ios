import Foundation

struct DailyEngine {
    let rules: DailyRules

    /// Renvoie la liste des Tehilim du jour selon le mode.
    /// - `monthly` : utilise le calendrier hébraïque.
    /// - `weekly` : utilise le jour de la semaine grégorien.
    func psalmsForToday(mode: DailyMode, on date: Date = Date()) -> [Int] {
        switch mode {
        case .monthly:
            return monthlyPsalms(on: date)
        case .weekly:
            return weeklyPsalms(on: date)
        case .custom:
            return [] // V2
        }
    }

    private func monthlyPsalms(on date: Date) -> [Int] {
        var calendar = Calendar(identifier: .hebrew)
        calendar.timeZone = TimeZone.current
        let day = calendar.component(.day, from: date)
        // Si jour 30 absent du dictionnaire, fallback sur 29.
        if let psalms = rules.modes.monthly.days[String(day)] { return psalms }
        return rules.modes.monthly.days["29"] ?? []
    }

    private func weeklyPsalms(on date: Date) -> [Int] {
        let weekday = Weekday.from(date)
        return rules.modes.weekly.days[weekday.rawValue] ?? []
    }
}

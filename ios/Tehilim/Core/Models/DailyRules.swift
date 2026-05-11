import Foundation

enum DailyMode: String, Codable, CaseIterable, Identifiable {
    case monthly, weekly, custom
    var id: String { rawValue }

    var label: String {
        switch self {
        case .monthly: return String(localized: "Cycle mensuel")
        case .weekly:  return String(localized: "Jour de la semaine")
        case .custom:  return String(localized: "Personnalisé")
        }
    }
}

enum Weekday: String, Codable, CaseIterable {
    case sunday, monday, tuesday, wednesday, thursday, friday, saturday

    static func from(_ d: Date, calendar: Calendar = .current) -> Weekday {
        let comp = calendar.component(.weekday, from: d) // 1 = Sunday
        return Weekday.allCases[comp - 1]
    }
}

struct DailyRules: Codable {
    struct Modes: Codable {
        let monthly: Monthly
        let weekly: Weekly
        let custom: Custom?
    }
    struct Monthly: Codable {
        let days: [String: [Int]]          // "1".."29"/"30"
    }
    struct Weekly: Codable {
        let days: [String: [Int]]          // "sunday"... "saturday"
    }
    struct Custom: Codable {
        let enabled: Bool
    }

    let version: String
    let modes: Modes

    static let empty = DailyRules(
        version: "0.0.0",
        modes: .init(
            monthly: .init(days: [:]),
            weekly: .init(days: [:]),
            custom: .init(enabled: false)
        )
    )
}

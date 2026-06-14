import Foundation
import Combine

/// Suivi de la série de lecture (V2.3) — « X jours d'affilée ».
///
/// Chaque jour où l'utilisateur ouvre un Tehilim est enregistré (clé locale
/// `reading.days`, au format `yyyy-MM-dd`). On en dérive :
/// - `current` : nombre de jours consécutifs se terminant aujourd'hui (ou hier,
///   tolérance d'un jour pour ne pas « casser » la série avant la fin de journée) ;
/// - `best` : meilleure série historique ;
/// - `total` : nombre total de jours de lecture.
///
/// Local par appareil (pas de sync iCloud) — une série est personnelle au rythme
/// de lecture sur l'appareil. Léger : on borne l'historique à 730 jours.
final class ReadingStreakStore: ObservableObject {
    @Published private(set) var current: Int = 0
    @Published private(set) var best: Int = 0
    @Published private(set) var total: Int = 0

    private let key = "reading.days"
    private var days: [String] = []   // "yyyy-MM-dd", triées croissantes, uniques

    private static let fmt: DateFormatter = {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    init() {
        days = (UserDefaults.standard.array(forKey: key) as? [String]) ?? []
        recompute()
    }

    /// Marque « aujourd'hui » comme jour de lecture (idempotent).
    func markReadToday() {
        let today = Self.fmt.string(from: Date())
        guard !days.contains(today) else { return }
        days.append(today)
        if days.count > 730 { days = Array(days.suffix(730)) }
        UserDefaults.standard.set(days, forKey: key)
        recompute()
    }

    // MARK: - Calcul

    private func recompute() {
        let set = Set(days)
        total = set.count

        let cal = Calendar(identifier: .gregorian)
        let todayStr = Self.fmt.string(from: Date())
        let yesterdayStr = Self.fmt.string(from: cal.date(byAdding: .day, value: -1, to: Date())!)

        // Série courante : on part d'aujourd'hui si lu, sinon d'hier (tolérance),
        // sinon 0. Puis on remonte tant que le jour précédent est présent.
        var cur = 0
        var cursor: Date?
        if set.contains(todayStr) { cursor = Date() }
        else if set.contains(yesterdayStr) { cursor = cal.date(byAdding: .day, value: -1, to: Date()) }
        while let c = cursor, set.contains(Self.fmt.string(from: c)) {
            cur += 1
            cursor = cal.date(byAdding: .day, value: -1, to: c)
        }
        current = cur

        // Meilleure série : plus longue suite de dates consécutives.
        let sorted = set.compactMap { Self.fmt.date(from: $0) }.sorted()
        var bestRun = 0, run = 0
        var prev: Date?
        for d in sorted {
            if let p = prev, cal.dateComponents([.day], from: p, to: d).day == 1 {
                run += 1
            } else {
                run = 1
            }
            bestRun = max(bestRun, run)
            prev = d
        }
        best = max(bestRun, current)
    }
}

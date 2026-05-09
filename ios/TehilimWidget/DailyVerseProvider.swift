import WidgetKit
import Foundation

struct DailyVerseEntry: TimelineEntry {
    let date: Date
    /// Tous les Tehilim du jour avec leur paire (id arabe, lettre hébraïque).
    let todayPsalms: [PsalmRef]
    /// Mode de lecture actif (mensuel ou hebdomadaire) — affiché en sous-titre.
    let mode: DailyMode
    /// Le verset accent (premier verset du premier Tehilim) pour les tailles medium/large.
    let firstVerseHebrew: String
    let firstVerseFR: String?
    /// Date hébraïque formatée pour le bandeau supérieur.
    let hebrewDate: String

    struct PsalmRef: Hashable {
        let id: Int
        let hebrewNumber: String
    }

    static let placeholder = DailyVerseEntry(
        date: Date(),
        todayPsalms: [
            .init(id: 35, hebrewNumber: "לה"),
            .init(id: 36, hebrewNumber: "לו"),
            .init(id: 37, hebrewNumber: "לז"),
            .init(id: 38, hebrewNumber: "לח"),
        ],
        mode: .monthly,
        firstVerseHebrew: "לְדָוִד רִיבָה יְהוָה אֶת יְרִיבַי לְחַם אֶת לֹחֲמָי",
        firstVerseFR: "De David. O Eternel, combats mes adversaires, fais la guerre à ceux qui me font la guerre.",
        hebrewDate: "ז׳ באייר ה׳תשפ״ו"
    )
}

struct DailyVerseProvider: TimelineProvider {
    func placeholder(in context: Context) -> DailyVerseEntry {
        DailyVerseEntry.placeholder
    }

    func getSnapshot(in context: Context, completion: @escaping (DailyVerseEntry) -> Void) {
        completion(currentEntry(now: Date()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<DailyVerseEntry>) -> Void) {
        let now = Date()
        let entry = currentEntry(now: now)

        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = .current
        let nextRefresh = cal.startOfDay(for: now.addingTimeInterval(60 * 60 * 24))

        let timeline = Timeline(entries: [entry], policy: .after(nextRefresh))
        completion(timeline)
    }

    private func currentEntry(now: Date) -> DailyVerseEntry {
        let psalms = WidgetDataLoader.loadPsalms()
        let rules = WidgetDataLoader.loadDailyRules()
        let engine = DailyEngine(rules: rules)
        let mode = readDailyMode()
        let ids = engine.psalmsForToday(mode: mode, on: now)

        // Construit la liste des psaumes du jour avec leurs numéros hébreux.
        let todayPsalms: [DailyVerseEntry.PsalmRef] = ids.compactMap { id in
            guard let p = psalms.first(where: { $0.id == id }) else { return nil }
            return .init(id: p.id, hebrewNumber: p.hebrewNumber)
        }

        // Verset d'accent : 1er verset du 1er Tehilim de la liste.
        var firstHebrew = ""
        var firstFR: String? = nil
        if let first = ids.first,
           let psalm = psalms.first(where: { $0.id == first }),
           let v = psalm.verses.first {
            firstHebrew = v.hebrew
            firstFR = v.translationFR
        }

        let hebrewDate = HebrewDateFormatter.formatted(now).hebrew

        return DailyVerseEntry(
            date: now,
            todayPsalms: todayPsalms,
            mode: mode,
            firstVerseHebrew: firstHebrew,
            firstVerseFR: firstFR,
            hebrewDate: hebrewDate
        )
    }

    private func readDailyMode() -> DailyMode {
        if let raw = AppGroup.userDefaults.string(forKey: AppGroup.Keys.dailyMode),
           let mode = DailyMode(rawValue: raw) {
            return mode
        }
        return .monthly
    }
}

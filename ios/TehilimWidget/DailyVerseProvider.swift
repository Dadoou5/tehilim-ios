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
    /// Mode Chabbat actif → le widget affiche « Chabbat Chalom » au lieu du
    /// contenu. `shabbatEndsAt` = heure de fin (Havdala) si connue.
    var isShabbat: Bool = false
    var shabbatEndsAt: Date? = nil

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

        // Rafraîchit à la prochaine bascule Chabbat (fin de Chabbat ou prochain
        // allumage des bougies) si connue, sinon au prochain minuit.
        let shabbatState = readShabbatState(now: now)
        let nextSwitch = shabbatState.shouldDisplay ? shabbatState.endsAt : shabbatState.nextStartsAt
        let midnight = cal.startOfDay(for: now.addingTimeInterval(60 * 60 * 24))
        let nextRefresh: Date = {
            guard let nextSwitch, nextSwitch > now else { return midnight }
            return min(nextSwitch.addingTimeInterval(60), midnight)
        }()

        let timeline = Timeline(entries: [entry], policy: .after(nextRefresh))
        completion(timeline)
    }

    /// Lit la position partagée par l'app et évalue l'état Chabbat. Si le mode
    /// est désactivé ou la position absente, retourne « pas Chabbat ».
    private func readShabbatState(now: Date) -> ShabbatState {
        let d = AppGroup.userDefaults
        guard d.bool(forKey: AppGroup.Keys.shabbatEnabled) else {
            return ShabbatState(isShabbat: false, endsAt: nil, nextStartsAt: nil)
        }
        let lat = d.double(forKey: AppGroup.Keys.shabbatLatitude)
        let lon = d.double(forKey: AppGroup.Keys.shabbatLongitude)
        guard lat != 0 || lon != 0 else {
            return ShabbatState(isShabbat: false, endsAt: nil, nextStartsAt: nil)
        }
        return ShabbatCalculator.state(now: now, coordinate: .init(latitude: lat, longitude: lon))
    }

    private func currentEntry(now: Date) -> DailyVerseEntry {
        // Mode Chabbat prioritaire : on masque le contenu du jour.
        let shabbat = readShabbatState(now: now)
        if shabbat.shouldDisplay {
            return DailyVerseEntry(
                date: now,
                todayPsalms: [],
                mode: readDailyMode(),
                firstVerseHebrew: "",
                firstVerseFR: nil,
                hebrewDate: HebrewDateFormatter.formatted(now).hebrew,
                isShabbat: true,
                shabbatEndsAt: shabbat.endsAt
            )
        }
        return contentEntry(now: now)
    }

    private func contentEntry(now: Date) -> DailyVerseEntry {
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

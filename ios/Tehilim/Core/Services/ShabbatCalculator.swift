import Foundation

/// Coordonnée géographique (degrés décimaux).
struct GeoCoordinate: Equatable, Codable {
    let latitude: Double
    let longitude: Double
}

/// Une ville pré-enregistrée pour le repli « pas de GPS ».
struct ShabbatCity: Identifiable, Equatable {
    let id: String
    let nameFR: String
    let coordinate: GeoCoordinate
}

/// État Chabbat à un instant donné.
struct ShabbatState: Equatable {
    /// True si l'instant évalué tombe entre l'allumage des bougies (vendredi)
    /// et la sortie de Chabbat / Havdala (samedi soir).
    let isShabbat: Bool
    /// Fin de Chabbat (Havdala) si `isShabbat`, sinon nil.
    let endsAt: Date?
    /// Début du Chabbat (allumage des bougies) — renseigné en Chabbat ET en
    /// pré-Chabbat (pour afficher l'horaire d'entrée à l'avance).
    let startedAt: Date?
    /// Pré-Chabbat : on est dans l'heure qui précède l'entrée. L'écran s'affiche
    /// à l'avance (horaires d'entrée + sortie), sans que Chabbat ait commencé.
    let isPreShabbat: Bool
    /// Prochaine apparition de l'écran (= entrée − 1 h) si on n'est ni en
    /// Chabbat ni en pré-Chabbat — sert à planifier le réveil.
    let nextStartsAt: Date?

    init(isShabbat: Bool, endsAt: Date? = nil, startedAt: Date? = nil,
         isPreShabbat: Bool = false, nextStartsAt: Date? = nil) {
        self.isShabbat = isShabbat
        self.endsAt = endsAt
        self.startedAt = startedAt
        self.isPreShabbat = isPreShabbat
        self.nextStartsAt = nextStartsAt
    }

    /// True si l'écran « Chabbat Chalom » doit s'afficher (pré-Chabbat ou Chabbat).
    var shouldDisplay: Bool { isShabbat || isPreShabbat }
}

/// Calcul du coucher du soleil (algorithme NOAA / Almanac) et de la fenêtre de
/// Chabbat. **Pur** (pas d'UIKit/CoreLocation) → partagé app ↔ widget.
///
/// Règles retenues (cf. décision produit) :
/// - Début : vendredi, coucher du soleil − 18 min (allumage des bougies).
/// - Fin   : samedi, sortie des étoiles (Tzeit) = soleil à 8,5° sous l'horizon
///   (défaut Hebcal « 3 étoiles moyennes »), calculée selon la position.
enum ShabbatCalculator {

    /// Offset allumage des bougies avant le coucher (minutes).
    static let candleLightingOffsetMinutes = 18.0
    /// Havdala = sortie des étoiles : angle de dépression solaire standard
    /// (8,5°, défaut Hebcal). Dépend de la position et de la date.
    static let havdalahDepressionDegrees = 8.5
    /// Repli quand le soleil n'atteint pas 8,5° sous l'horizon (hautes
    /// latitudes en été) : coucher + 72 min.
    static let havdalahFallbackOffsetMinutes = 72.0
    /// L'écran s'affiche dès 1 h avant l'entrée (pré-Chabbat) pour informer
    /// l'utilisateur des horaires d'entrée et de sortie.
    static let preShabbatLeadSeconds: TimeInterval = 3600

    /// Liste de villes pour le repli sans GPS (id stable, ne pas renommer).
    static let cities: [ShabbatCity] = [
        .init(id: "jerusalem", nameFR: "Jérusalem", coordinate: .init(latitude: 31.7683, longitude: 35.2137)),
        .init(id: "telaviv",   nameFR: "Tel Aviv",  coordinate: .init(latitude: 32.0853, longitude: 34.7818)),
        .init(id: "paris",     nameFR: "Paris",      coordinate: .init(latitude: 48.8566, longitude: 2.3522)),
        .init(id: "marseille", nameFR: "Marseille",  coordinate: .init(latitude: 43.2965, longitude: 5.3698)),
        .init(id: "lyon",      nameFR: "Lyon",        coordinate: .init(latitude: 45.7640, longitude: 4.8357)),
        .init(id: "nice",      nameFR: "Nice",        coordinate: .init(latitude: 43.7102, longitude: 7.2620)),
        .init(id: "strasbourg", nameFR: "Strasbourg", coordinate: .init(latitude: 48.5734, longitude: 7.7521)),
        .init(id: "toulouse",  nameFR: "Toulouse",    coordinate: .init(latitude: 43.6047, longitude: 1.4442)),
        .init(id: "london",    nameFR: "Londres",     coordinate: .init(latitude: 51.5074, longitude: -0.1278)),
        .init(id: "brussels",  nameFR: "Bruxelles",   coordinate: .init(latitude: 50.8503, longitude: 4.3517)),
        .init(id: "geneva",    nameFR: "Genève",      coordinate: .init(latitude: 46.2044, longitude: 6.1432)),
        .init(id: "montreal",  nameFR: "Montréal",    coordinate: .init(latitude: 45.5019, longitude: -73.5674)),
        .init(id: "newyork",   nameFR: "New York",    coordinate: .init(latitude: 40.7128, longitude: -74.0060)),
        .init(id: "losangeles", nameFR: "Los Angeles", coordinate: .init(latitude: 34.0522, longitude: -118.2437))
    ]

    static func city(id: String) -> ShabbatCity? { cities.first { $0.id == id } }

    // MARK: - État Chabbat

    /// Évalue l'état Chabbat à `now` pour la position `coord`.
    static func state(now: Date, coordinate: GeoCoordinate,
                      timeZone: TimeZone = .current) -> ShabbatState {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone

        let weekday = cal.component(.weekday, from: now) // 1=dim … 6=ven, 7=sam

        // Fenêtre d'affichage = [entrée − 1 h, havdala]. Bloquant à partir de
        // l'entrée (bougies) ; informatif (pré-Chabbat) dans l'heure avant.
        if weekday == 6 { // vendredi
            if let friday = dateOnly(now, cal: cal),
               let saturday = cal.date(byAdding: .day, value: 1, to: friday),
               let candle = candleLighting(on: friday, coordinate: coordinate, cal: cal),
               let havdalah = havdalah(on: saturday, coordinate: coordinate, cal: cal) {
                if now >= candle {
                    return ShabbatState(isShabbat: true, endsAt: havdalah, startedAt: candle)
                }
                let preStart = candle.addingTimeInterval(-preShabbatLeadSeconds)
                if now >= preStart {
                    return ShabbatState(isShabbat: false, endsAt: havdalah,
                                        startedAt: candle, isPreShabbat: true)
                }
                return ShabbatState(isShabbat: false, nextStartsAt: preStart)
            }
        } else if weekday == 7 { // samedi
            if let saturday = dateOnly(now, cal: cal),
               let friday = cal.date(byAdding: .day, value: -1, to: saturday),
               let havdalah = havdalah(on: saturday, coordinate: coordinate, cal: cal) {
                if now <= havdalah {
                    // startedAt = allumage des bougies de la veille (vendredi).
                    let candle = candleLighting(on: friday, coordinate: coordinate, cal: cal)
                    return ShabbatState(isShabbat: true, endsAt: havdalah, startedAt: candle)
                }
            }
        }

        // Hors fenêtre : prochaine apparition de l'écran = entrée − 1 h.
        let next = nextCandleLighting(after: now, coordinate: coordinate, cal: cal)
        return ShabbatState(isShabbat: false,
                            nextStartsAt: next?.addingTimeInterval(-preShabbatLeadSeconds))
    }

    private static func nextCandleLighting(after now: Date, coordinate: GeoCoordinate,
                                           cal: Calendar) -> Date? {
        guard let today = dateOnly(now, cal: cal) else { return nil }
        // Cherche le prochain vendredi (0…7 jours) dont l'allumage est > now.
        for offset in 0...7 {
            guard let day = cal.date(byAdding: .day, value: offset, to: today) else { continue }
            if cal.component(.weekday, from: day) == 6,
               let candle = candleLighting(on: day, coordinate: coordinate, cal: cal),
               candle > now {
                return candle
            }
        }
        return nil
    }

    private static func candleLighting(on friday: Date, coordinate: GeoCoordinate,
                                       cal: Calendar) -> Date? {
        guard let s = sunset(on: friday, coordinate: coordinate, cal: cal) else { return nil }
        return s.addingTimeInterval(-candleLightingOffsetMinutes * 60)
    }

    private static func havdalah(on saturday: Date, coordinate: GeoCoordinate,
                                 cal: Calendar) -> Date? {
        // Sortie des étoiles : soleil à 8,5° sous l'horizon (zénith 98,5°).
        if let tzeit = eveningEvent(on: saturday, coordinate: coordinate, cal: cal,
                                    zenith: 90.0 + havdalahDepressionDegrees) {
            return tzeit
        }
        // Repli (le soleil ne descend pas jusqu'à 8,5° près du solstice à
        // haute latitude) : coucher + 72 min.
        guard let s = sunset(on: saturday, coordinate: coordinate, cal: cal) else { return nil }
        return s.addingTimeInterval(havdalahFallbackOffsetMinutes * 60)
    }

    private static func dateOnly(_ date: Date, cal: Calendar) -> Date? {
        cal.startOfDay(for: date)
    }

    // MARK: - Coucher du soleil (NOAA / Almanac)

    /// Coucher du soleil pour la date civile de `day` (dans `cal.timeZone`) à
    /// la position donnée. Retourne l'instant absolu (Date), ou nil aux
    /// latitudes polaires où le soleil ne se couche pas ce jour-là.
    static func sunset(on day: Date, coordinate: GeoCoordinate, cal: Calendar) -> Date? {
        // Coucher du soleil = zénith officiel 90°50' (réfraction + rayon).
        eveningEvent(on: day, coordinate: coordinate, cal: cal, zenith: 90.833)
    }

    /// Événement solaire du soir pour un `zenith` donné (90,833 = coucher ;
    /// > 90 = crépuscule / sortie des étoiles selon l'angle de dépression).
    static func eveningEvent(on day: Date, coordinate: GeoCoordinate, cal: Calendar,
                             zenith: Double) -> Date? {
        let comps = cal.dateComponents([.year, .month, .day], from: day)
        guard let year = comps.year, let month = comps.month, let dayNum = comps.day else { return nil }
        return sunsetUTC(year: year, month: month, day: dayNum,
                         latitude: coordinate.latitude, longitude: coordinate.longitude,
                         timeZone: cal.timeZone, zenith: zenith)
    }

    private static func deg2rad(_ d: Double) -> Double { d * .pi / 180 }
    private static func rad2deg(_ r: Double) -> Double { r * 180 / .pi }
    private static func mod(_ a: Double, _ b: Double) -> Double { let r = a.truncatingRemainder(dividingBy: b); return r < 0 ? r + b : r }

    /// Algorithme « Sunrise/Sunset » de l'Almanac (NOAA), précision ~1 min.
    private static func sunsetUTC(year: Int, month: Int, day: Int,
                                  latitude: Double, longitude: Double,
                                  timeZone: TimeZone, zenith: Double = 90.833) -> Date? {
        // 1. Jour de l'année
        let N1 = floor(275.0 * Double(month) / 9.0)
        let N2 = floor((Double(month) + 9.0) / 12.0)
        let N3 = 1.0 + floor((Double(year) - 4.0 * floor(Double(year) / 4.0) + 2.0) / 3.0)
        let N = N1 - (N2 * N3) + Double(day) - 30.0

        let lngHour = longitude / 15.0
        // Approximation de l'heure (sunset → 18h)
        let t = N + ((18.0 - lngHour) / 24.0)

        // Anomalie moyenne du soleil
        let M = (0.9856 * t) - 3.289
        // Longitude vraie
        var L = M + (1.916 * sin(deg2rad(M))) + (0.020 * sin(deg2rad(2 * M))) + 282.634
        L = mod(L, 360.0)
        // Ascension droite
        var RA = rad2deg(atan(0.91764 * tan(deg2rad(L))))
        RA = mod(RA, 360.0)
        // Même quadrant que L
        let Lquadrant = floor(L / 90.0) * 90.0
        let RAquadrant = floor(RA / 90.0) * 90.0
        RA = (RA + (Lquadrant - RAquadrant)) / 15.0
        // Déclinaison
        let sinDec = 0.39782 * sin(deg2rad(L))
        let cosDec = cos(asin(sinDec))
        // Angle horaire — `zenith` passé en paramètre (90,833 = coucher,
        // 98,5 = sortie des étoiles à 8,5°).
        let cosH = (cos(deg2rad(zenith)) - (sinDec * sin(deg2rad(latitude)))) /
                   (cosDec * cos(deg2rad(latitude)))
        if cosH > 1 || cosH < -1 { return nil } // pas de coucher (polaire)
        // Sunset → H = acos(cosH)
        let H = rad2deg(acos(cosH)) / 15.0
        let T = H + RA - (0.06571 * t) - 6.622
        let UT = mod(T - lngHour, 24.0)

        // Construit l'instant : UTC du jour (year,month,day) à UT heures.
        let hour = Int(floor(UT))
        let minuteFull = (UT - Double(hour)) * 60.0
        let minute = Int(floor(minuteFull))
        let second = Int((minuteFull - Double(minute)) * 60.0)

        var utc = Calendar(identifier: .gregorian)
        utc.timeZone = TimeZone(identifier: "UTC")!
        var dc = DateComponents()
        dc.year = year; dc.month = month; dc.day = day
        dc.hour = hour; dc.minute = minute; dc.second = second
        guard let candidate = utc.date(from: dc) else { return nil }

        // Correction de bord : aux longitudes où le coucher tombe de l'autre
        // côté de minuit UTC, l'instant calculé peut être décalé d'un jour.
        // On choisit le décalage (−1/0/+1 j) dont la date LOCALE colle au jour
        // visé.
        var targetCal = Calendar(identifier: .gregorian)
        targetCal.timeZone = timeZone
        for shift in [0, 1, -1] {
            let c = candidate.addingTimeInterval(Double(shift) * 86400.0)
            let lc = targetCal.dateComponents([.year, .month, .day], from: c)
            if lc.year == year && lc.month == month && lc.day == day {
                return c
            }
        }
        return candidate
    }
}

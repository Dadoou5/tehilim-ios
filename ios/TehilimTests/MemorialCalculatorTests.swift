import XCTest
@testable import Tehilim

/// Tests du calculateur d'azcara — V1.10.7.
///
/// Couvre :
/// - Conversion civile → hébraïque
/// - Calcul de la prochaine azcara civile
/// - Cas spéciaux : Adar / Adar II, Heshvan 30, Kislev 30, Adar I 30
/// - Rétrocompat decoding de SavedPrayerIntent (sans les champs Commémoration)
final class MemorialCalculatorTests: XCTestCase {

    // Helpers

    private func civil(_ y: Int, _ m: Int, _ d: Int) -> Date {
        var dc = DateComponents()
        dc.year = y; dc.month = m; dc.day = d; dc.hour = 12
        return Calendar(identifier: .gregorian).date(from: dc)!
    }

    private func hebrewYMD(_ date: Date) -> (Int, Int, Int) {
        let ymd = MemorialCalculator.hebrewYMD(from: date)
        return (ymd.year, ymd.month, ymd.day)
    }

    // MARK: - Conversion civile → hébraïque

    func testConvertCivilToHebrewKnownDate() {
        // 24 mai 2026 → 8 Sivan 5786 (vérifié sur app + sources externes)
        let (y, _, _) = hebrewYMD(civil(2026, 5, 24))
        XCTAssertEqual(y, 5786)
    }

    // MARK: - Cycle Méton (années embolismiques)

    func testLeapYearMetonic() {
        // Année 5784 = embolismique (Adar I + Adar II)
        XCTAssertTrue(MemorialCalculator.isLeap(year: 5784))
        // Année 5785 = commune
        XCTAssertFalse(MemorialCalculator.isLeap(year: 5785))
        // Année 5786 = commune
        XCTAssertFalse(MemorialCalculator.isLeap(year: 5786))
        // Année 5787 = embolismique
        XCTAssertTrue(MemorialCalculator.isLeap(year: 5787))
    }

    // MARK: - Adaptation du mois (Adar / Adar II)
    //
    // Indexation Apple Hebrew (= ICU 1-indexée, STABLE) :
    //   6 = Adar I (leap-only), 7 = Adar (commune unique OU leap = Adar II),
    //   8 = Nisan (toujours), 10 = Sivan (toujours), etc.

    func testAdarFromNonLeapToLeap_observedInAdarII_sameIndex() {
        // Source Adar année commune (mois 7) → cible embolismique : Adar II
        // (mois 7 aussi, indexation stable). C'est l'interprétation
        // « observée en Adar II » de la tradition Ashkenazi.
        let m = MemorialCalculator.adjustedMonth(
            sourceMonth: 7, sourceLeap: false, targetLeap: true
        )
        XCTAssertEqual(m, 7)
    }

    func testAdarIFromLeapToNonLeap_collapsesToSingleAdar() {
        // Source Adar I (leap, mois 6) → cible commune : Adar I n'existe pas,
        // observée en Adar unique (mois 7).
        let m = MemorialCalculator.adjustedMonth(
            sourceMonth: 6, sourceLeap: true, targetLeap: false
        )
        XCTAssertEqual(m, 7)
    }

    func testAdarIIFromLeapToNonLeap_stillIndex7() {
        // Source Adar II (leap, mois 7) → cible commune : Adar unique (7).
        let m = MemorialCalculator.adjustedMonth(
            sourceMonth: 7, sourceLeap: true, targetLeap: false
        )
        XCTAssertEqual(m, 7)
    }

    func testAdarI_LeapToLeap_stillAdarI() {
        let m = MemorialCalculator.adjustedMonth(
            sourceMonth: 6, sourceLeap: true, targetLeap: true
        )
        XCTAssertEqual(m, 6)
    }

    func testPostAdarMonths_stableIndices() {
        // Nisan = 8 toujours, Sivan = 10 toujours, peu importe leap.
        // Plus de décalage post-Adar dans l'indexation stable.
        XCTAssertEqual(MemorialCalculator.adjustedMonth(sourceMonth: 8, sourceLeap: false, targetLeap: true), 8)
        XCTAssertEqual(MemorialCalculator.adjustedMonth(sourceMonth: 8, sourceLeap: true, targetLeap: false), 8)
        XCTAssertEqual(MemorialCalculator.adjustedMonth(sourceMonth: 10, sourceLeap: false, targetLeap: true), 10)
        XCTAssertEqual(MemorialCalculator.adjustedMonth(sourceMonth: 10, sourceLeap: true, targetLeap: false), 10)
    }

    // MARK: - Adaptation du jour (rollover 30 → 1 du mois suivant)

    /// Trouve dynamiquement une année hébraïque où `month` a `expectedDays`
    /// jours — évite d'hardcoder des numéros d'année qui pourraient devenir
    /// invalides si la table calendaire d'iOS évoluait.
    private func findYear(month: Int, withDays expectedDays: Int) -> Int? {
        (5780...5810).first { year in
            MemorialCalculator.daysInMonth(month: month, year: year) == expectedDays
        }
    }

    func testHeshvan30Rollover() {
        // Source : 30 Heshvan (mois 2). Cible : année où Heshvan = 29 jours.
        // Attendu : 1 Kislev (mois 3, jour 1).
        guard let year = findYear(month: 2, withDays: 29) else {
            XCTFail("No year with Heshvan = 29 days found in 5780..5810"); return
        }
        let (m, d) = MemorialCalculator.adjustedDay(
            sourceDay: 30, sourceMonth: 2, sourceLeap: false,
            targetMonth: 2, targetYear: year
        )
        XCTAssertEqual(m, 3)
        XCTAssertEqual(d, 1)
    }

    func testKislev30Rollover() {
        // Source : 30 Kislev (mois 3). Cible : année où Kislev = 29 jours.
        // Attendu : 1 Tevet (mois 4, jour 1).
        guard let year = findYear(month: 3, withDays: 29) else {
            XCTFail("No year with Kislev = 29 days found in 5780..5810"); return
        }
        let (m, d) = MemorialCalculator.adjustedDay(
            sourceDay: 30, sourceMonth: 3, sourceLeap: false,
            targetMonth: 3, targetYear: year
        )
        XCTAssertEqual(m, 4)
        XCTAssertEqual(d, 1)
    }

    func testAdarI30ToNonLeap_observedOn30Shevat() {
        // Source : 30 Adar I (leap, mois 6). Cible : année commune.
        // Attendu : 30 Shevat (mois 5).
        // Note : on simule un targetMonth quelconque non-leap (e.g. 7 = Adar).
        let (m, d) = MemorialCalculator.adjustedDay(
            sourceDay: 30, sourceMonth: 6, sourceLeap: true,
            targetMonth: 7, targetYear: 5786 // non-leap
        )
        XCTAssertEqual(m, 5, "30 Adar I + non-leap → 30 Shevat (mois 5)")
        XCTAssertEqual(d, 30)
    }

    func testRegularDayNoRollover() {
        // 15 Tishri (mois 1) → toujours 15 Tishri.
        let (m, d) = MemorialCalculator.adjustedDay(
            sourceDay: 15, sourceMonth: 1, sourceLeap: false,
            targetMonth: 1, targetYear: 5786
        )
        XCTAssertEqual(m, 1)
        XCTAssertEqual(d, 15)
    }

    // MARK: - Calcul end-to-end

    func testNextYahrzeit_FutureDateInSameHebrewYear() {
        // Décès 1 Janvier 2025 (≈ 1 Shevat 5785).
        // Au 1 Juillet 2025, l'azcara de Shevat est dans le futur (Shevat 5786 → Janvier 2026).
        let death = civil(2025, 1, 1)
        let now = civil(2025, 7, 1)
        let next = MemorialCalculator.nextYahrzeit(deathCivil: death, now: now)
        XCTAssertNotNil(next)
        XCTAssertGreaterThan(next!, now)
        // Doit être à environ ~1 an après le décès (avec une marge ±60 jours
        // pour absorber le décalage Greg/Heb).
        let daysApart = Calendar(identifier: .gregorian)
            .dateComponents([.day], from: death, to: next!).day ?? 0
        XCTAssertGreaterThan(daysApart, 365 - 60)
        XCTAssertLessThan(daysApart, 365 + 60)
    }

    func testNextYahrzeit_OffsetsByOneYearWhenJustPassed() {
        // Décès 24 mai 2024. Au 25 mai 2026, l'anniversaire 2026 vient de
        // passer → next doit être l'azcara 2027.
        let death = civil(2024, 5, 24)
        let now = civil(2026, 5, 25)
        let next = MemorialCalculator.nextYahrzeit(deathCivil: death, now: now)
        XCTAssertNotNil(next)
        // Doit être au moins ~10 mois plus tard.
        let monthsApart = Calendar(identifier: .gregorian)
            .dateComponents([.month], from: now, to: next!).month ?? 0
        XCTAssertGreaterThan(monthsApart, 10)
    }

    // MARK: - Rétrocompat sérialisation

    func testSavedPrayerIntent_DecodesLegacyPayloadWithoutMemorialFields() throws {
        // JSON tel qu'écrit par V1.10.6 (sans civilDateOfDeath, etc.).
        let legacyJSON = """
        {
          "id": "11111111-2222-3333-4444-555555555555",
          "title": "Lelouy Nichmat — יוסף בן שרה",
          "prayerType": "defunt",
          "relativeFirstName": "יוסף",
          "relationType": "ben",
          "motherFirstName": "שרה",
          "generatedLetters": [],
          "createdAt": 768000000
        }
        """.data(using: .utf8)!

        let decoder = JSONDecoder()
        let intent = try decoder.decode(SavedPrayerIntent.self, from: legacyJSON)

        XCTAssertEqual(intent.relativeFirstName, "יוסף")
        XCTAssertNil(intent.civilDateOfDeath)
        XCTAssertNil(intent.hebrewDateOfDeath)
        XCTAssertFalse(intent.remindersEnabled)
        XCTAssertTrue(intent.notifySevenDaysBefore)  // défaut
        XCTAssertTrue(intent.notifySameDay)          // défaut
    }

    func testSavedPrayerIntent_RoundTripWithMemorialFields() throws {
        let intent = SavedPrayerIntent(
            title: "Lelouy Nichmat — Test",
            prayerType: .defunt,
            relativeFirstName: "יוסף",
            relationType: .ben,
            motherFirstName: "שרה",
            generatedLetters: [],
            civilDateOfDeath: civil(2024, 1, 15),
            hebrewDateOfDeath: HebrewYMD(year: 5784, month: 5, day: 5),
            remindersEnabled: true,
            notifySevenDaysBefore: true,
            notifySameDay: false
        )
        let data = try JSONEncoder().encode(intent)
        let back = try JSONDecoder().decode(SavedPrayerIntent.self, from: data)
        XCTAssertEqual(back.remindersEnabled, true)
        XCTAssertEqual(back.notifySameDay, false)
        XCTAssertEqual(back.hebrewDateOfDeath?.year, 5784)
    }
}

extension MemorialCalculatorTests {
    /// Test de régression — bug remonté en V1.10.7 dev :
    /// décès 24 mai 2020 (1 Sivan 5780), attendue prochaine azcara
    /// en juin 2027 (1 Sivan 5787, année embolismique). L'ancienne
    /// logique faisait un mauvais shift de +1 sur Sivan (mois 10)
    /// → renvoyait Tammuz (11) → 6 juillet 2027, faux.
    func testRegression_SivanInLeapTarget_24May2020() {
        let death = civil(2020, 5, 24)
        let now = civil(2026, 5, 24)
        let next = MemorialCalculator.nextYahrzeit(deathCivil: death, now: now)
        XCTAssertNotNil(next)
        let nextMonth = Calendar(identifier: .gregorian)
            .dateComponents([.month, .year], from: next!)
        XCTAssertEqual(nextMonth.year, 2027)
        XCTAssertEqual(nextMonth.month, 6, "Doit être en juin 2027 (1 Sivan 5787), pas juillet")
    }

    /// Test de régression — l'azcara du JOUR ne doit pas être skippée
    /// vers l'année suivante simplement parce qu'on l'ouvre l'après-midi.
    ///
    /// Cas user : décès 19 mai 2021 (8 Sivan 5781). Le 24 mai 2026 = 8 Sivan
    /// 5786, donc l'azcara tombe aujourd'hui. Quel que soit l'heure dans
    /// la journée, le calculateur doit retourner aujourd'hui (et non l'an
    /// prochain).
    func testRegression_AzcaraTodayKeptAsToday_19May2021() {
        let death = civil(2021, 5, 19)
        // Simule "ouverture de l'app à 18h le 24 mai 2026" — passé midi
        // mais l'azcara du jour ne doit pas être skippée.
        var nowDC = DateComponents()
        nowDC.year = 2026; nowDC.month = 5; nowDC.day = 24; nowDC.hour = 18
        let now = Calendar(identifier: .gregorian).date(from: nowDC)!

        let next = MemorialCalculator.nextYahrzeit(deathCivil: death, now: now)
        XCTAssertNotNil(next)
        let nextDC = Calendar(identifier: .gregorian)
            .dateComponents([.year, .month, .day], from: next!)
        XCTAssertEqual(nextDC.year, 2026, "Doit rester en 2026 (aujourd'hui), pas 2027")
        XCTAssertEqual(nextDC.month, 5)
        XCTAssertEqual(nextDC.day, 24)
    }

    /// Test de régression — l'azcara D'HIER doit basculer sur l'année
    /// suivante. C'est exactement le scénario user : « lorsque je viens
    /// de passer une azcara ». Vérifie que le calcul saute bien.
    func testRegression_AzcaraYesterdayJumpsToNextYear_19May2021() {
        let death = civil(2021, 5, 19)
        // Hier était l'azcara (24 mai 2026 = 8 Sivan 5786) — aujourd'hui
        // est le 25 mai 2026 = 9 Sivan 5786, donc passé.
        var nowDC = DateComponents()
        nowDC.year = 2026; nowDC.month = 5; nowDC.day = 25; nowDC.hour = 10
        let now = Calendar(identifier: .gregorian).date(from: nowDC)!

        let next = MemorialCalculator.nextYahrzeit(deathCivil: death, now: now)
        XCTAssertNotNil(next)
        let nextDC = Calendar(identifier: .gregorian)
            .dateComponents([.year, .month, .day], from: next!)
        // Prochaine azcara = 8 Sivan 5787 = environ 5/6 juin 2027 selon
        // mapping civil de Apple (year leap, donc Sivan toujours mois 10).
        XCTAssertEqual(nextDC.year, 2027, "Doit sauter à 2027, pas rester en 2026")
        XCTAssertEqual(nextDC.month, 6, "Doit être en juin 2027 (Sivan 5787)")
    }

    /// Test : juste après minuit (00:30) le lendemain de l'azcara.
    /// Cas limite — vérifie qu'on ne renvoie pas encore l'azcara
    /// d'hier juste parce qu'il est très tôt le matin.
    func testRegression_JustPastMidnightAfterAzcara_19May2021() {
        let death = civil(2021, 5, 19)
        var nowDC = DateComponents()
        nowDC.year = 2026; nowDC.month = 5; nowDC.day = 25; nowDC.hour = 0; nowDC.minute = 30
        let now = Calendar(identifier: .gregorian).date(from: nowDC)!

        let next = MemorialCalculator.nextYahrzeit(deathCivil: death, now: now)
        XCTAssertNotNil(next)
        let nextDC = Calendar(identifier: .gregorian)
            .dateComponents([.year, .month, .day], from: next!)
        XCTAssertEqual(nextDC.year, 2027,
                       "À 00:30 le 25 mai, l'azcara du 24 mai est passée → 2027")
    }
}

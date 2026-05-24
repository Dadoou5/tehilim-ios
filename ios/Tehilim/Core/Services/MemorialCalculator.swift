import Foundation

/// Calcule la prochaine azcara (anniversaire hébraïque du décès) à partir
/// d'une date civile de décès, en appliquant les règles traditionnelles :
///
///   - Adar / Adar II : dans une année embolismique, l'azcara d'un décès
///     survenu en Adar (année commune) est observée en Adar II.
///   - Heshvan 30 / Kislev 30 : si l'année cible n'a pas 30 jours dans
///     ce mois, l'azcara passe au 1er du mois suivant.
///   - Adar I 30 : si l'année cible n'est pas embolismique, l'azcara est
///     observée le 30 Shevat (tradition la plus répandue).
///
/// La logique est volontairement isolée de l'UI pour être testable. Le
/// pendant Android (`MemorialCalculator.kt`) reproduit exactement le même
/// algorithme avec ICU `HebrewCalendar`.
///
/// V1.10.7 — feature Commémoration.
enum MemorialCalculator {

    /// API publique : civile → civile. Retourne la date civile de la
    /// prochaine azcara strictement postérieure à `now`.
    static func nextYahrzeit(deathCivil: Date, now: Date = Date()) -> Date? {
        let death = hebrewYMD(from: deathCivil)
        let today = hebrewYMD(from: now)
        let sourceLeap = isLeap(year: death.year)

        // Borne de sécurité : en pratique 1 itération, max 2 si l'azcara
        // de l'année courante est déjà passée.
        for offset in 0..<3 {
            let targetYear = today.year + offset
            let targetLeap = isLeap(year: targetYear)
            let tm = adjustedMonth(
                sourceMonth: death.month,
                sourceLeap: sourceLeap,
                targetLeap: targetLeap
            )
            let (fm, fd) = adjustedDay(
                sourceDay: death.day,
                sourceMonth: death.month,
                sourceLeap: sourceLeap,
                targetMonth: tm,
                targetYear: targetYear
            )
            if let candidate = civilDate(year: targetYear, month: fm, day: fd),
               candidate > now {
                return candidate
            }
        }
        return nil
    }

    /// Convertit une date civile en composantes hébraïques.
    static func hebrewYMD(from date: Date) -> HebrewYMD {
        let cal = Calendar(identifier: .hebrew)
        let c = cal.dateComponents([.year, .month, .day], from: date)
        return HebrewYMD(year: c.year ?? 0, month: c.month ?? 0, day: c.day ?? 0)
    }

    /// Convertit une date hébraïque (Y/M/D Apple-indexée) en date civile
    /// (midi local pour éviter les ambiguïtés de bordure de jour).
    static func civilDate(year: Int, month: Int, day: Int) -> Date? {
        let cal = Calendar(identifier: .hebrew)
        var dc = DateComponents()
        dc.year = year
        dc.month = month
        dc.day = day
        dc.hour = 12
        return cal.date(from: dc)
    }

    /// Cycle de Méton : 7 années embolismiques sur 19. La formule
    /// `(7y + 1) mod 19 < 7` est la règle classique.
    static func isLeap(year: Int) -> Bool {
        ((7 * year + 1) % 19) < 7
    }

    /// Nombre de jours dans `month` pour `year`. Utilise NSCalendar pour
    /// rester aligné sur l'implémentation Apple (qui gère deficient /
    /// regular / complete year — l'utilisateur n'a pas à connaître).
    static func daysInMonth(month: Int, year: Int) -> Int {
        let cal = Calendar(identifier: .hebrew)
        var dc = DateComponents()
        dc.year = year
        dc.month = month
        dc.day = 1
        guard let date = cal.date(from: dc),
              let range = cal.range(of: .day, in: .month, for: date) else {
            return 29
        }
        return range.count
    }

    // MARK: - Règles métier (internes, exposées en `internal` pour les tests)

    /// Adaptation du mois entre année source et année cible.
    ///
    /// Indexation Apple Hebrew :
    /// - Année commune : 1=Tishri, 2=Heshvan, 3=Kislev, 4=Tevet, 5=Shevat,
    ///                   **6=Adar** (unique), 7=Nisan, ..., 12=Elul.
    /// - Année embolismique : 1=Tishri, ..., 5=Shevat,
    ///                        **6=Adar I**, **7=Adar II**, 8=Nisan, ..., 13=Elul.
    static func adjustedMonth(
        sourceMonth: Int,
        sourceLeap: Bool,
        targetLeap: Bool
    ) -> Int {
        if sourceLeap {
            // Source en Adar I (mois 6) : cible en Adar I si année cible
            // embolismique, sinon Adar unique (mois 6 non-leap).
            if sourceMonth == 6 { return 6 }
            // Source en Adar II (mois 7) : Adar II en leap, Adar unique
            // en non-leap.
            if sourceMonth == 7 { return targetLeap ? 7 : 6 }
            // Source post-Adar (Nisan..Elul = 8..13 en leap). En non-leap,
            // ils sont décalés à 7..12.
            if !targetLeap && sourceMonth >= 8 { return sourceMonth - 1 }
            return sourceMonth
        } else {
            // Source en Adar unique (non-leap, mois 6) : observée en
            // Adar II si cible embolismique (tradition Ashkenazi standard).
            if sourceMonth == 6 { return targetLeap ? 7 : 6 }
            // Source post-Adar (Nisan..Elul = 7..12 en non-leap). En leap,
            // décalés à 8..13 (insertion d'Adar II).
            if targetLeap && sourceMonth >= 7 { return sourceMonth + 1 }
            return sourceMonth
        }
    }

    /// Adaptation du jour quand le mois cible est plus court (29 vs 30).
    static func adjustedDay(
        sourceDay: Int,
        sourceMonth: Int,
        sourceLeap: Bool,
        targetMonth: Int,
        targetYear: Int
    ) -> (month: Int, day: Int) {
        let daysAvail = daysInMonth(month: targetMonth, year: targetYear)
        if sourceDay <= daysAvail {
            return (targetMonth, sourceDay)
        }
        // sourceDay == 30, targetMonth a 29 jours.
        // Cas spécial : 30 Adar I (source leap, sourceMonth == 6) →
        // 30 Shevat (mois 5) en année cible non-leap (Adar I n'existe pas).
        if sourceLeap && sourceMonth == 6 && sourceDay == 30 {
            return (5, 30)
        }
        // Règle standard : 30 Heshvan → 1 Kislev, 30 Kislev → 1 Tevet.
        return (targetMonth + 1, 1)
    }
}

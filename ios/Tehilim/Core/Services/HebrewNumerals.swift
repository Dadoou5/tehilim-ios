import Foundation

/// Conversion entre entiers (1...400) et notation hébraïque par gematria.
/// Respecte les conventions standards : 15 = טו, 16 = טז (et non יה / יו).
enum HebrewNumerals {

    private static let mapping: [(value: Int, letter: String)] = [
        (400, "ת"), (300, "ש"), (200, "ר"), (100, "ק"),
        (90,  "צ"), (80,  "פ"), (70,  "ע"), (60,  "ס"),
        (50,  "נ"), (40,  "מ"), (30,  "ל"), (20,  "כ"),
        (10,  "י"),
        (9,   "ט"), (8,   "ח"), (7,   "ז"), (6,   "ו"),
        (5,   "ה"), (4,   "ד"), (3,   "ג"), (2,   "ב"), (1, "א")
    ]

    /// Convertit un entier en lettres hébraïques (sans gershayim).
    static func toHebrew(_ n: Int) -> String {
        guard n > 0 else { return "" }
        var remainder = n
        var result = ""

        // Cas spéciaux 15 et 16 — gérer la dizaine et garder l'unité spéciale.
        // On traite les centaines normalement, puis le reste avec règle 15/16.
        if remainder >= 100 {
            for (val, letter) in mapping where val >= 100 {
                while remainder >= val {
                    result += letter
                    remainder -= val
                }
            }
        }
        if remainder == 15 { return result + "טו" }
        if remainder == 16 { return result + "טז" }

        for (val, letter) in mapping where val < 100 {
            while remainder >= val {
                result += letter
                remainder -= val
            }
        }
        return result
    }

    /// Convertit une chaîne hébraïque en entier (best-effort).
    /// - Ignore les caractères non-hébreu (gershayim, espaces, ponctuation).
    static func toInt(_ s: String) -> Int? {
        let cleaned = s.unicodeScalars.filter { isHebrewLetter($0) }.map(Character.init).map(String.init).joined()
        guard !cleaned.isEmpty else { return nil }

        let values: [Character: Int] = [
            "א": 1, "ב": 2, "ג": 3, "ד": 4, "ה": 5,
            "ו": 6, "ז": 7, "ח": 8, "ט": 9, "י": 10,
            "כ": 20, "ך": 20, "ל": 30, "מ": 40, "ם": 40,
            "נ": 50, "ן": 50, "ס": 60, "ע": 70, "פ": 80, "ף": 80,
            "צ": 90, "ץ": 90, "ק": 100, "ר": 200, "ש": 300, "ת": 400
        ]

        var total = 0
        for ch in cleaned {
            guard let v = values[ch] else { return nil }
            total += v
        }
        return total > 0 ? total : nil
    }

    private static func isHebrewLetter(_ scalar: Unicode.Scalar) -> Bool {
        // Plage des lettres hébraïques + finales
        return (0x05D0...0x05EA).contains(scalar.value)
    }
}

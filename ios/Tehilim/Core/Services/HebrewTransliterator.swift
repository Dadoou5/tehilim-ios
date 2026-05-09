import Foundation

/// Transcrit un texte hébreu vocalisé (avec nikud) en phonétique française sépharade.
///
/// **Approche** : règles algorithmiques caractère-par-caractère, pré-passe sur les noms divins.
/// **Système** : sépharade (cible francophone). Achkénaze envisageable en V2.
/// **Limites connues** :
/// - Le ḥiriq-yod (`xִי`) génère parfois "iy" au lieu de "i".
/// - Le shva est par défaut silencieux (pas de distinction shva na' / shva nah).
/// - Le qamats qatan (אָ → "o") n'est pas détecté contextuellement → traduit "a".
/// - Mappiq dans le ה final non détecté.
///
/// Toute transcription affichée à l'utilisateur est étiquetée "transcription assistée"
/// dans la déclaration d'accessibilité et la page Sources.
enum HebrewTransliterator {

    static func transliterate(_ source: String) -> String {
        var s = replaceDivineNames(in: source)
        s = walk(s)
        return s.replacingOccurrences(of: " +", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespaces)
    }

    // MARK: - Walk

    private static func walk(_ s: String) -> String {
        var output = ""
        let scalars = Array(s.unicodeScalars)
        var i = 0
        var lastVowel: String = ""    // pour matres lectionis (ḥiriq-yod, etc.)

        while i < scalars.count {
            let sc = scalars[i]

            if isLetter(sc) {
                var diacritics: [Unicode.Scalar] = []
                var j = i + 1
                while j < scalars.count, isDiacritic(scalars[j]) {
                    diacritics.append(scalars[j])
                    j += 1
                }

                // Mater lectionis : yod nu après ḥiriq → muet (la voyelle "i" est déjà émise).
                // Idem vav nu après holam (rare en hébreu vocalisé moderne).
                if sc.value == 0x05D9, diacritics.isEmpty, lastVowel == "i" {
                    i = j
                    continue
                }

                let result = transliterate(letter: sc, diacritics: diacritics)
                output.append(result)
                lastVowel = vowel(diacritics)
                i = j
            } else if sc.value == 0x05BE {                    // maqqef ־
                output.append("-")
                lastVowel = ""
                i += 1
            } else if sc.value == 0x05C0 || sc.value == 0x05C3 { // paseq, sof pasuq
                output.append(" ")
                lastVowel = ""
                i += 1
            } else if isWhitespace(sc) {
                output.append(" ")
                lastVowel = ""
                i += 1
            } else if isDiacritic(sc) {
                i += 1
            } else {
                output.append(String(sc))
                lastVowel = ""
                i += 1
            }
        }
        return output
    }

    // MARK: - Single-letter mapping

    private static func transliterate(letter: Unicode.Scalar, diacritics: [Unicode.Scalar]) -> String {
        let hasSinDot   = diacritics.contains { $0.value == 0x05C2 }
        let hasDagesh   = diacritics.contains { $0.value == 0x05BC }
        let hasHolam    = diacritics.contains { $0.value == 0x05B9 }

        // Vav qui sert de voyelle :
        // - וֹ (vav + holam) → "o"
        // - וּ (vav + dagesh seul) → "ou"
        if letter.value == 0x05D5 {
            if hasHolam { return "o" }
            if hasDagesh, !diacritics.contains(where: { isVowelMark($0) && $0.value != 0x05BC }) {
                return "ou"
            }
        }

        let consonant: String
        switch letter.value {
        case 0x05D0: consonant = ""                              // alef silencieux
        case 0x05D1: consonant = hasDagesh ? "b" : "v"
        case 0x05D2: consonant = "g"
        case 0x05D3: consonant = "d"
        case 0x05D4: consonant = "h"
        case 0x05D5: consonant = "v"
        case 0x05D6: consonant = "z"
        case 0x05D7: consonant = "h"
        case 0x05D8: consonant = "t"
        case 0x05D9: consonant = "y"
        case 0x05DA, 0x05DB: consonant = hasDagesh ? "k" : "kh"
        case 0x05DC: consonant = "l"
        case 0x05DD, 0x05DE: consonant = "m"
        case 0x05DF, 0x05E0: consonant = "n"
        case 0x05E1: consonant = "s"
        case 0x05E2: consonant = ""                              // ayin silencieux (sépharade)
        case 0x05E3, 0x05E4: consonant = hasDagesh ? "p" : "f"
        case 0x05E5, 0x05E6: consonant = "ts"
        case 0x05E7: consonant = "k"
        case 0x05E8: consonant = "r"
        case 0x05E9: consonant = hasSinDot ? "s" : "ch"
        case 0x05EA: consonant = "t"
        default: consonant = ""
        }

        return consonant + vowel(diacritics)
    }

    private static func vowel(_ diacritics: [Unicode.Scalar]) -> String {
        for d in diacritics {
            switch d.value {
            case 0x05B0: return ""           // shva — silencieux par défaut
            case 0x05B1: return "é"          // hataf segol
            case 0x05B2: return "a"          // hataf patah
            case 0x05B3: return "o"          // hataf qamats
            case 0x05B4: return "i"          // hiriq
            case 0x05B5: return "é"          // tsere
            case 0x05B6: return "é"          // segol
            case 0x05B7: return "a"          // patah
            case 0x05B8: return "a"          // qamats sépharade
            case 0x05B9: return "o"          // holam
            case 0x05BB: return "ou"         // qubuts
            case 0x05C7: return "o"          // qamats qatan
            default: continue
            }
        }
        return ""
    }

    private static func isVowelMark(_ s: Unicode.Scalar) -> Bool {
        let v = s.value
        return (0x05B0...0x05BB).contains(v) || v == 0x05C7
    }

    // MARK: - Divine names pre-pass

    /// Remplace les Tétragrammes (יהוה) par "Adonaï" avant la transcription lettre-par-lettre.
    /// Le pattern matche en sautant les nikud. Les autres noms (אלהים, אדני) sont laissés à la
    /// transcription algorithmique : ils donnent naturellement "élohim" / "adonaï".
    private static func replaceDivineNames(in s: String) -> String {
        var output = ""
        let scalars = Array(s.unicodeScalars)
        var i = 0
        while i < scalars.count {
            if let match = matchTetragrammaton(at: i, in: scalars) {
                output.append("Adonaï")
                i += match
            } else {
                output.append(String(scalars[i]))
                i += 1
            }
        }
        return output
    }

    private static let tetragrammatonLetters: [UInt32] = [0x05D9, 0x05D4, 0x05D5, 0x05D4]

    private static func matchTetragrammaton(at start: Int, in scalars: [Unicode.Scalar]) -> Int? {
        var i = start
        var p = 0
        while p < tetragrammatonLetters.count, i < scalars.count {
            let s = scalars[i]
            if isLetter(s) {
                if s.value != tetragrammatonLetters[p] { return nil }
                p += 1
                i += 1
            } else if isDiacritic(s) {
                i += 1
            } else {
                return nil
            }
        }
        return p == tetragrammatonLetters.count ? (i - start) : nil
    }

    // MARK: - Char classification

    private static func isLetter(_ s: Unicode.Scalar) -> Bool {
        (0x05D0...0x05EA).contains(s.value)
    }

    private static func isDiacritic(_ s: Unicode.Scalar) -> Bool {
        let v = s.value
        return (0x0591...0x05BD).contains(v)
            || v == 0x05BF
            || (0x05C1...0x05C2).contains(v)
            || (0x05C4...0x05C5).contains(v)
            || v == 0x05C7
    }

    private static func isWhitespace(_ s: Unicode.Scalar) -> Bool {
        s.value == 0x20 || s.value == 0x09 || s.value == 0x0A || s.value == 0xA0
    }
}

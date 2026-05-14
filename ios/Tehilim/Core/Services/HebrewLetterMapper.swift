import Foundation

/// Utilitaires de manipulation des lettres hébraïques pour la lecture personnalisée.
///
/// Deux responsabilités :
/// 1. **Mapping final → base** : les 5 lettres finales (ך, ם, ן, ף, ץ) sont mappées
///    à leur forme de base pour pouvoir retrouver la section correspondante du
///    Tehilim 119 (qui n'a que 22 sections, une par lettre de base).
/// 2. **Validation Hebrew** : filtre les caractères pour ne garder que les lettres
///    hébraïques (plage Unicode 0x05D0 – 0x05EA).
enum HebrewLetterMapper {

    /// Table de correspondance lettres finales → lettre de base.
    /// Source : norme typographique hébraïque standard.
    static let finalToBase: [Character: Character] = [
        "ך": "כ",
        "ם": "מ",
        "ן": "נ",
        "ף": "פ",
        "ץ": "צ"
    ]

    /// Plage Unicode des lettres hébraïques (base + finales).
    /// Inclut : א ב ג ד ה ו ז ח ט י ך כ ל ם מ ן נ ס ע ף פ ץ צ ק ר ש ת
    private static let hebrewLetterRange: ClosedRange<UInt32> = 0x05D0...0x05EA

    /// Convertit un caractère en sa forme de base (les finales deviennent leur base).
    /// Les caractères non-hébreux sont retournés inchangés.
    static func toBase(_ c: Character) -> Character {
        finalToBase[c] ?? c
    }

    /// Convertit toute une chaîne en lettres de base, filtrant les non-hébreu.
    /// Utile pour générer la séquence de lecture.
    static func baseLetters(in string: String) -> [Character] {
        string.compactMap { c in
            guard isHebrewLetter(c) else { return nil }
            return toBase(c)
        }
    }

    /// `true` si le caractère est une lettre hébraïque (base ou finale).
    static func isHebrewLetter(_ c: Character) -> Bool {
        guard let scalar = c.unicodeScalars.first else { return false }
        return hebrewLetterRange.contains(scalar.value)
    }

    /// Filtre une chaîne pour ne garder que les lettres hébraïques.
    /// Utilisé en temps réel dans les `TextField` pour rejeter toute saisie
    /// non-hébraïque (latin, chiffres, ponctuation, …).
    static func filterHebrew(_ string: String) -> String {
        String(string.filter { isHebrewLetter($0) })
    }

    /// Valide qu'un prénom est composé d'au moins 1 lettre hébraïque (et rien d'autre).
    static func isValidHebrewName(_ string: String) -> Bool {
        let trimmed = string.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return false }
        return trimmed.allSatisfy { isHebrewLetter($0) }
    }
}

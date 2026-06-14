import Foundation

/// Une occurrence trouvée dans le texte d'un verset (V2.3 — recherche plein-texte).
struct VerseTextMatch: Identifiable {
    let psalm: Psalm
    let verse: Verse
    let snippet: String          // extrait à afficher (hébreu ou traduction)
    var id: String { verse.id }
}

struct SearchQueryResult {
    let exactMatch: Psalm?
    let suggestions: [Psalm]
    let interpretation: String?
    /// V2.3 — occurrences du terme dans le texte des Tehilim (hébreu + traduction).
    var textMatches: [VerseTextMatch] = []
}

struct SearchInterpreter {
    let repository: PsalmRepository

    private static let stripWords: Set<String> = [
        "tehilim", "tehillim", "psaume", "psaumes", "psalm", "תהילים", "תהלים"
    ]

    /// Plafond d'occurrences plein-texte (perf + lisibilité de la liste).
    private static let maxTextMatches = 60

    func interpret(_ rawQuery: String) -> SearchQueryResult {
        let trimmed = rawQuery.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !trimmed.isEmpty else {
            return .init(exactMatch: nil, suggestions: defaultSuggestions(), interpretation: nil)
        }

        // 1. Retirer les mots prefix
        var cleaned = trimmed
        for word in Self.stripWords {
            cleaned = cleaned.replacingOccurrences(of: word, with: " ")
        }
        cleaned = cleaned
            .replacingOccurrences(of: "\"", with: "")
            .replacingOccurrences(of: "'", with: "")
            .replacingOccurrences(of: "״", with: "")
            .replacingOccurrences(of: "׳", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)

        // 2. Tentative numéro arabe
        if let arabicNumber = extractArabicNumber(cleaned), let psalm = repository.psalm(id: arabicNumber) {
            return .init(
                exactMatch: psalm,
                suggestions: nearby(id: psalm.id),
                interpretation: "Tehilim \(psalm.id) · \(psalm.hebrewNumber)"
            )
        }

        // 3. Tentative hébraïque (numéro en lettres)
        if let hebrewNumber = HebrewNumerals.toInt(cleaned), let psalm = repository.psalm(id: hebrewNumber) {
            return .init(
                exactMatch: psalm,
                suggestions: nearby(id: psalm.id),
                interpretation: "Tehilim \(psalm.id) · \(psalm.hebrewNumber)"
            )
        }

        // 4. Recherche plein-texte (V2.3) — dès 2 caractères, sur le texte hébreu
        //    (sans nikud/téamim) et la traduction de la langue active.
        let textMatches = trimmed.count >= 2 ? searchText(rawQuery) : []
        return .init(exactMatch: nil, suggestions: textMatches.isEmpty ? defaultSuggestions() : [],
                     interpretation: nil, textMatches: textMatches)
    }

    // MARK: - Recherche plein-texte

    private func searchText(_ rawQuery: String) -> [VerseTextMatch] {
        // Normalisation hébraïque : on retire nikud + téamim des deux côtés
        // pour une recherche tolérante (l'utilisateur tape rarement la vocalisation).
        let hebQuery = Self.stripHebrewDiacritics(rawQuery)
            .trimmingCharacters(in: .whitespacesAndNewlines)
        // Normalisation latine : minuscules + suppression des accents.
        let latinQuery = rawQuery.folding(options: [.diacriticInsensitive, .caseInsensitive],
                                          locale: .current)
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let hasHebrew = hebQuery.unicodeScalars.contains { (0x05D0...0x05EA).contains($0.value) }
        let lang = AppLocale.translationLanguage

        var matches: [VerseTextMatch] = []
        for psalm in repository.allPsalms {
            for verse in psalm.verses {
                var snippet: String?
                if hasHebrew, !hebQuery.isEmpty {
                    let stripped = Self.stripHebrewDiacritics(verse.hebrew)
                    if stripped.contains(hebQuery) { snippet = verse.hebrew }
                }
                if snippet == nil, !hasHebrew, latinQuery.count >= 2,
                   let t = verse.translation(for: lang) {
                    let folded = t.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
                    if folded.contains(latinQuery) { snippet = t }
                }
                if let snippet {
                    matches.append(VerseTextMatch(psalm: psalm, verse: verse,
                                                  snippet: Self.trim(snippet)))
                    if matches.count >= Self.maxTextMatches { return matches }
                }
            }
        }
        return matches
    }

    /// Retire nikud (U+05B0–U+05BD, U+05BF, U+05C1–U+05C2, U+05C4–U+05C5, U+05C7)
    /// et téamim (U+0591–U+05AF) — tout le bloc de marques hébraïques.
    static func stripHebrewDiacritics(_ s: String) -> String {
        String(String.UnicodeScalarView(s.unicodeScalars.filter { scalar in
            !(0x0591...0x05CF).contains(scalar.value)
        }))
    }

    private static func trim(_ s: String, limit: Int = 90) -> String {
        s.count <= limit ? s : String(s.prefix(limit)) + "…"
    }

    // MARK: - Helpers

    private func extractArabicNumber(_ s: String) -> Int? {
        let digits = s.filter { $0.isNumber }
        guard !digits.isEmpty, let n = Int(digits), (1...150).contains(n) else { return nil }
        return n
    }

    private func nearby(id: Int) -> [Psalm] {
        let candidates = [id - 1, id + 1].compactMap { repository.psalm(id: $0) }
        return candidates
    }

    private func defaultSuggestions() -> [Psalm] {
        [23, 27, 91, 121, 130, 150].compactMap { repository.psalm(id: $0) }
    }
}

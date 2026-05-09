import Foundation

struct SearchQueryResult {
    let exactMatch: Psalm?
    let suggestions: [Psalm]
    let interpretation: String?
}

struct SearchInterpreter {
    let repository: PsalmRepository

    private static let stripWords: Set<String> = [
        "tehilim", "tehillim", "psaume", "psaumes", "psalm", "תהילים", "תהלים"
    ]

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

        // 3. Tentative hébraïque
        if let hebrewNumber = HebrewNumerals.toInt(cleaned), let psalm = repository.psalm(id: hebrewNumber) {
            return .init(
                exactMatch: psalm,
                suggestions: nearby(id: psalm.id),
                interpretation: "Tehilim \(psalm.id) · \(psalm.hebrewNumber)"
            )
        }

        // 4. Aucun résultat exact
        return .init(exactMatch: nil, suggestions: defaultSuggestions(), interpretation: nil)
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

import Foundation

/// Chargeur minimaliste pour le widget — lit les JSONs embarqués dans le bundle de l'extension.
enum WidgetDataLoader {

    private struct PsalmsEnvelope: Decodable { let psalms: [Psalm] }

    /// Renvoie tous les psaumes du corpus. Cache en mémoire pendant la durée de vie de l'extension.
    static func loadPsalms() -> [Psalm] {
        if let cached = _cachedPsalms { return cached }
        guard let url = Bundle.main.url(forResource: "psalms", withExtension: "json"),
              let data = try? Data(contentsOf: url) else {
            return []
        }
        let env = try? JSONDecoder().decode(PsalmsEnvelope.self, from: data)
        _cachedPsalms = env?.psalms ?? []
        return _cachedPsalms ?? []
    }
    private static var _cachedPsalms: [Psalm]?

    /// Renvoie les règles de lecture quotidienne.
    static func loadDailyRules() -> DailyRules {
        if let cached = _cachedRules { return cached }
        guard let url = Bundle.main.url(forResource: "daily_reading_rules", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let rules = try? JSONDecoder().decode(DailyRules.self, from: data) else {
            return DailyRules.empty
        }
        _cachedRules = rules
        return rules
    }
    private static var _cachedRules: DailyRules?
}

import Foundation

protocol ContentLoading {
    func loadPsalms() throws -> [Psalm]
    func loadLifeCases() throws -> [LifeCase]
    func loadPsalm119Sections() throws -> [Psalm119Section]
    func loadDailyRules() throws -> DailyRules
}

enum ContentLoaderError: Error {
    case fileNotFound(String)
    case decodeFailed(String, underlying: Error)
}

/// Charge le contenu embarqué dans le bundle de l'app.
struct BundledContentLoader: ContentLoading {

    private let bundle: Bundle

    init(bundle: Bundle = .main) {
        self.bundle = bundle
    }

    func loadPsalms() throws -> [Psalm] {
        let envelope: PsalmsEnvelope = try decode("psalms")
        return envelope.psalms
    }

    func loadLifeCases() throws -> [LifeCase] {
        let envelope: LifeCasesEnvelope = try decode("life_cases")
        return envelope.categories
    }

    func loadPsalm119Sections() throws -> [Psalm119Section] {
        let envelope: Psalm119Envelope = try decode("psalm_119_sections")
        return envelope.sections
    }

    func loadDailyRules() throws -> DailyRules {
        try decode("daily_reading_rules")
    }

    // MARK: - Helpers

    private func decode<T: Decodable>(_ resourceName: String) throws -> T {
        guard let url = bundle.url(forResource: resourceName, withExtension: "json") else {
            throw ContentLoaderError.fileNotFound(resourceName)
        }
        do {
            let data = try Data(contentsOf: url)
            return try JSONDecoder().decode(T.self, from: data)
        } catch {
            throw ContentLoaderError.decodeFailed(resourceName, underlying: error)
        }
    }
}

private struct PsalmsEnvelope: Decodable { let psalms: [Psalm] }
private struct LifeCasesEnvelope: Decodable { let categories: [LifeCase] }
private struct Psalm119Envelope: Decodable { let sections: [Psalm119Section] }

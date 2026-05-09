import Foundation

final class LifeCaseRepository {
    struct Group: Identifiable {
        let title: String
        let cases: [LifeCase]
        var id: String { title }
    }

    let cases: [LifeCase]
    init(cases: [LifeCase]) { self.cases = cases }

    func find(id: String) -> LifeCase? { cases.first { $0.id == id } }

    /// Cas regroupés par section, dans l'ordre canonique.
    /// Si un cas n'a pas de section déclarée, il finit dans "Autres".
    var grouped: [Group] {
        let order = [
            "Cycle de vie",
            "Santé et épreuves",
            "Spiritualité",
            "Communauté et calendrier",
            "Autres",
        ]
        var byName: [String: [LifeCase]] = [:]
        for c in cases {
            let name = c.section ?? "Autres"
            byName[name, default: []].append(c)
        }
        return order.compactMap { name in
            guard let list = byName[name], !list.isEmpty else { return nil }
            return Group(title: name, cases: list)
        }
    }
}

final class Psalm119Repository {
    let sections: [Psalm119Section]
    init(sections: [Psalm119Section]) {
        self.sections = sections.sorted { $0.index < $1.index }
    }
    func section(at index: Int) -> Psalm119Section? {
        sections.first { $0.index == index }
    }
}

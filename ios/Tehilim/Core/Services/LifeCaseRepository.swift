import Foundation

final class LifeCaseRepository {
    struct Group: Identifiable {
        let title: String      // titre français (clé canonique)
        let titleEN: String    // titre anglais
        let titleHE: String    // titre hébreu
        let cases: [LifeCase]
        var id: String { title }

        var localizedTitle: String {
            LifeCase.pick(fr: title, en: titleEN, he: titleHE)
        }
    }

    let cases: [LifeCase]
    init(cases: [LifeCase]) { self.cases = cases }

    func find(id: String) -> LifeCase? { cases.first { $0.id == id } }

    /// Cas regroupés par section, dans l'ordre canonique.
    var grouped: [Group] {
        let order: [(fr: String, en: String, he: String)] = [
            ("Cycle de vie", "Life cycle", "מעגל החיים"),
            ("Santé et épreuves", "Health and trials", "בריאות וקשיים"),
            ("Spiritualité", "Spirituality", "רוחניות"),
            ("Communauté et calendrier", "Community and calendar", "קהילה ולוח השנה"),
            ("Autres", "Other", "אחר"),
        ]
        var byName: [String: [LifeCase]] = [:]
        for c in cases {
            let name = c.section ?? "Autres"
            byName[name, default: []].append(c)
        }
        return order.compactMap { triple in
            guard let list = byName[triple.fr], !list.isEmpty else { return nil }
            return Group(title: triple.fr, titleEN: triple.en, titleHE: triple.he, cases: list)
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
    /// Retrouve la section correspondant à une lettre (forme de base, ex. « א », « ב »).
    /// Utilisé par la lecture personnalisée pour mapper chaque lettre de la séquence
    /// à sa section du Tehilim 119.
    func section(forLetter letter: String) -> Psalm119Section? {
        sections.first { $0.letter == letter }
    }
}

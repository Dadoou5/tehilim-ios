import Foundation

final class PsalmRepository {
    private let psalmsById: [Int: Psalm]
    let allPsalms: [Psalm]

    init(psalms: [Psalm]) {
        self.allPsalms = psalms.sorted { $0.id < $1.id }
        self.psalmsById = Dictionary(uniqueKeysWithValues: psalms.map { ($0.id, $0) })
    }

    func psalm(id: Int) -> Psalm? { psalmsById[id] }

    func psalms(inBook book: Int) -> [Psalm] {
        guard let range = Psalm.bookRanges[book] else { return [] }
        return allPsalms.filter { range.contains($0.id) }
    }

    func neighbors(of id: Int) -> (prev: Int?, next: Int?) {
        let prev = (1...150).contains(id - 1) ? id - 1 : nil
        let next = (1...150).contains(id + 1) ? id + 1 : nil
        return (prev, next)
    }
}

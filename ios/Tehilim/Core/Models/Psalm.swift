import Foundation

struct Psalm: Codable, Identifiable, Hashable {
    let id: Int                  // 1...150
    let book: Int                // 1...5
    let hebrewNumber: String
    let hebrewTitle: String?
    let verses: [Verse]
    let tags: [String]
}

struct Verse: Codable, Identifiable, Hashable {
    let id: String               // "23:1"
    let number: Int
    let hebrewNumber: String
    let hebrew: String
    let translationFR: String?
}

extension Psalm {
    static let bookRanges: [Int: ClosedRange<Int>] = [
        1: 1...41,
        2: 42...72,
        3: 73...89,
        4: 90...106,
        5: 107...150
    ]

    static func book(forPsalmId id: Int) -> Int {
        for (book, range) in bookRanges where range.contains(id) { return book }
        return 1
    }
}

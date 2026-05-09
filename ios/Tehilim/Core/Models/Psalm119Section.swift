import Foundation

struct Psalm119Section: Codable, Identifiable, Hashable {
    let id: String
    let index: Int
    let letter: String
    let name: String
    let verseStart: Int
    let verseEnd: Int

    var versesRange: ClosedRange<Int> { verseStart...verseEnd }
}

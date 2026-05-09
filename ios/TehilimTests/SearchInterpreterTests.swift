import XCTest
@testable import Tehilim

final class SearchInterpreterTests: XCTestCase {

    private func makeInterpreter() -> SearchInterpreter {
        let psalms: [Psalm] = (1...150).map { id in
            Psalm(id: id,
                  book: Psalm.book(forPsalmId: id),
                  hebrewNumber: HebrewNumerals.toHebrew(id),
                  hebrewTitle: nil,
                  verses: [],
                  tags: [])
        }
        let repo = PsalmRepository(psalms: psalms)
        return SearchInterpreter(repository: repo)
    }

    func test_arabicNumber() {
        let r = makeInterpreter().interpret("23")
        XCTAssertEqual(r.exactMatch?.id, 23)
    }

    func test_hebrewNumber() {
        let r = makeInterpreter().interpret("כג")
        XCTAssertEqual(r.exactMatch?.id, 23)
    }

    func test_mixedQueryFR() {
        let r = makeInterpreter().interpret("Tehilim 23")
        XCTAssertEqual(r.exactMatch?.id, 23)

        let r2 = makeInterpreter().interpret("psaume 23")
        XCTAssertEqual(r2.exactMatch?.id, 23)
    }

    func test_mixedQueryHE() {
        let r = makeInterpreter().interpret("תהילים כג")
        XCTAssertEqual(r.exactMatch?.id, 23)
    }

    func test_outOfRange() {
        XCTAssertNil(makeInterpreter().interpret("0").exactMatch)
        XCTAssertNil(makeInterpreter().interpret("151").exactMatch)
    }

    func test_emptyQueryReturnsSuggestions() {
        let r = makeInterpreter().interpret("")
        XCTAssertNil(r.exactMatch)
        XCTAssertFalse(r.suggestions.isEmpty)
    }

    func test_garbageQuery() {
        let r = makeInterpreter().interpret("🎉🎂abc!!")
        XCTAssertNil(r.exactMatch)
    }

    func test_specialFifteenSixteen() {
        XCTAssertEqual(makeInterpreter().interpret("טו").exactMatch?.id, 15)
        XCTAssertEqual(makeInterpreter().interpret("טז").exactMatch?.id, 16)
    }
}

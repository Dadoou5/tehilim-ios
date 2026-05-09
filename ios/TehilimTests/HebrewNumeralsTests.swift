import XCTest
@testable import Tehilim

final class HebrewNumeralsTests: XCTestCase {

    func test_toHebrew_basicValues() {
        XCTAssertEqual(HebrewNumerals.toHebrew(1), "א")
        XCTAssertEqual(HebrewNumerals.toHebrew(10), "י")
        XCTAssertEqual(HebrewNumerals.toHebrew(23), "כג")
        XCTAssertEqual(HebrewNumerals.toHebrew(100), "ק")
        XCTAssertEqual(HebrewNumerals.toHebrew(150), "קנ")
    }

    func test_toHebrew_specialFifteenAndSixteen() {
        XCTAssertEqual(HebrewNumerals.toHebrew(15), "טו")
        XCTAssertEqual(HebrewNumerals.toHebrew(16), "טז")
        XCTAssertEqual(HebrewNumerals.toHebrew(115), "קטו")
        XCTAssertEqual(HebrewNumerals.toHebrew(116), "קטז")
    }

    func test_toInt_basicValues() {
        XCTAssertEqual(HebrewNumerals.toInt("א"), 1)
        XCTAssertEqual(HebrewNumerals.toInt("י"), 10)
        XCTAssertEqual(HebrewNumerals.toInt("כג"), 23)
        XCTAssertEqual(HebrewNumerals.toInt("קנ"), 150)
    }

    func test_toInt_special() {
        XCTAssertEqual(HebrewNumerals.toInt("טו"), 15)
        XCTAssertEqual(HebrewNumerals.toInt("טז"), 16)
    }

    func test_toInt_withGershayim() {
        XCTAssertEqual(HebrewNumerals.toInt("כ\"ג"), 23)
        XCTAssertEqual(HebrewNumerals.toInt("ק״נ"), 150)
    }

    func test_toInt_invalid() {
        XCTAssertNil(HebrewNumerals.toInt(""))
        XCTAssertNil(HebrewNumerals.toInt("abc"))
    }
}

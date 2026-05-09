import XCTest
@testable import Tehilim

final class HebrewTransliteratorTests: XCTestCase {

    func test_emptyString() {
        XCTAssertEqual(HebrewTransliterator.transliterate(""), "")
    }

    func test_nonHebrewIsPreserved() {
        XCTAssertEqual(HebrewTransliterator.transliterate("hello"), "hello")
    }

    func test_simpleWord_chalom() {
        // שָׁלוֹם
        let result = HebrewTransliterator.transliterate("שָׁלוֹם")
        XCTAssertEqual(result.lowercased(), "chalom")
    }

    func test_simpleWord_mizmor() {
        // מִזְמוֹר
        let result = HebrewTransliterator.transliterate("מִזְמוֹר")
        XCTAssertEqual(result.lowercased(), "mizmor")
    }

    func test_dagesh_distinguishes_b_v() {
        // בְּ vs בֲ — bet avec dagesh = b, sans = v.
        XCTAssertTrue(HebrewTransliterator.transliterate("בּ").lowercased().hasPrefix("b"))
        XCTAssertTrue(HebrewTransliterator.transliterate("ב").lowercased().hasPrefix("v"))
    }

    func test_shin_vs_sin() {
        // שׁ → "ch" ; שׂ → "s"
        XCTAssertEqual(HebrewTransliterator.transliterate("שׁ").lowercased(), "ch")
        XCTAssertEqual(HebrewTransliterator.transliterate("שׂ").lowercased(), "s")
    }

    func test_vavAsVowel_holam() {
        // בּוֹ → "bo"
        XCTAssertEqual(HebrewTransliterator.transliterate("בּוֹ").lowercased(), "bo")
    }

    func test_vavAsVowel_shuruq() {
        // בּוּ → "bou"
        XCTAssertEqual(HebrewTransliterator.transliterate("בּוּ").lowercased(), "bou")
    }

    func test_tetragrammaton_replaced_by_Adonai() {
        // יהוה (sans nikud) → "Adonaï"
        XCTAssertEqual(HebrewTransliterator.transliterate("יהוה"), "Adonaï")
        // יְהוָה (avec nikud) → "Adonaï"
        XCTAssertEqual(HebrewTransliterator.transliterate("יְהוָה"), "Adonaï")
    }

    func test_tetragrammaton_inSentence() {
        // "יְהוָה רֹעִי" → contient "Adonaï" suivi de "ro..."
        let result = HebrewTransliterator.transliterate("יְהוָה רֹעִי")
        XCTAssertTrue(result.contains("Adonaï"))
    }

    func test_qamats_isA_inSephardic() {
        // אָב → "av"
        XCTAssertEqual(HebrewTransliterator.transliterate("אָב").lowercased(), "av")
    }

    func test_alephAndAyin_areSilent() {
        // אֱלֹהִים → "élohim" ; ayin silencieux.
        let result = HebrewTransliterator.transliterate("אֱלֹהִים").lowercased()
        XCTAssertEqual(result, "élohim")
    }

    func test_tsadi_isTs() {
        // צ → ts
        XCTAssertTrue(HebrewTransliterator.transliterate("צ").lowercased().hasPrefix("ts"))
    }

    func test_finalLetters_treated_likeBase() {
        // ך final = kh sans dagesh ; ם final = m
        XCTAssertEqual(HebrewTransliterator.transliterate("ך").lowercased(), "kh")
        XCTAssertEqual(HebrewTransliterator.transliterate("ם").lowercased(), "m")
    }

    func test_maqqef_becomes_hyphen() {
        // עַל־כֵּן → contient un tiret
        let result = HebrewTransliterator.transliterate("עַל־כֵּן")
        XCTAssertTrue(result.contains("-"))
    }
}

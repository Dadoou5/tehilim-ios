import XCTest
@testable import Tehilim

/// Tests pour la lecture personnalisée du Tehilim 119.
final class PersonalizedReadingTests: XCTestCase {

    // MARK: - HebrewLetterMapper

    func test_mapper_finalLetters_convertToBase() {
        XCTAssertEqual(HebrewLetterMapper.toBase("ך"), "כ")
        XCTAssertEqual(HebrewLetterMapper.toBase("ם"), "מ")
        XCTAssertEqual(HebrewLetterMapper.toBase("ן"), "נ")
        XCTAssertEqual(HebrewLetterMapper.toBase("ף"), "פ")
        XCTAssertEqual(HebrewLetterMapper.toBase("ץ"), "צ")
    }

    func test_mapper_baseLetters_passThrough() {
        XCTAssertEqual(HebrewLetterMapper.toBase("א"), "א")
        XCTAssertEqual(HebrewLetterMapper.toBase("ת"), "ת")
    }

    func test_mapper_nonHebrew_passThrough() {
        // Caractères non-hébreu (latin) ne sont pas mappés.
        XCTAssertEqual(HebrewLetterMapper.toBase("a"), "a")
        XCTAssertEqual(HebrewLetterMapper.toBase("5"), "5")
    }

    func test_mapper_isHebrewLetter() {
        XCTAssertTrue(HebrewLetterMapper.isHebrewLetter("א"))
        XCTAssertTrue(HebrewLetterMapper.isHebrewLetter("ך"))
        XCTAssertTrue(HebrewLetterMapper.isHebrewLetter("ת"))
        XCTAssertFalse(HebrewLetterMapper.isHebrewLetter("a"))
        XCTAssertFalse(HebrewLetterMapper.isHebrewLetter(" "))
        XCTAssertFalse(HebrewLetterMapper.isHebrewLetter("1"))
    }

    func test_mapper_filterHebrew_keepsOnlyHebrew() {
        XCTAssertEqual(HebrewLetterMapper.filterHebrew("יוסף"), "יוסף")
        XCTAssertEqual(HebrewLetterMapper.filterHebrew("Yossef יוסף"), "יוסף")
        XCTAssertEqual(HebrewLetterMapper.filterHebrew("יוסף 123"), "יוסף")
        XCTAssertEqual(HebrewLetterMapper.filterHebrew("abc"), "")
    }

    func test_mapper_baseLetters_mapsFinalsAndDropsNonHebrew() {
        // « יצחק » → י צ ח ק (le צ pourrait être final mais ici c'est le ח final non, le ק final)
        // Plus pertinent : prendre « שלום » (avec ם final → מ)
        let result = HebrewLetterMapper.baseLetters(in: "שלום")
        XCTAssertEqual(result, ["ש", "ל", "ו", "מ"])
    }

    func test_mapper_baseLetters_handlesMixedFinalAndBase() {
        // « ירושלים » contient ם final (→ מ) et autres lettres de base
        let result = HebrewLetterMapper.baseLetters(in: "ירושלים")
        XCTAssertEqual(result, ["י", "ר", "ו", "ש", "ל", "י", "מ"])
    }

    func test_mapper_isValidHebrewName() {
        XCTAssertTrue(HebrewLetterMapper.isValidHebrewName("יוסף"))
        XCTAssertTrue(HebrewLetterMapper.isValidHebrewName("שרה"))
        XCTAssertFalse(HebrewLetterMapper.isValidHebrewName(""))
        XCTAssertFalse(HebrewLetterMapper.isValidHebrewName("   "))
        XCTAssertFalse(HebrewLetterMapper.isValidHebrewName("Yossef"))
        XCTAssertFalse(HebrewLetterMapper.isValidHebrewName("יוסף 123"))
    }

    // MARK: - LetterSequenceGenerator

    func test_generator_malade_doesNotAppendNeshama() {
        let seq = LetterSequenceGenerator.generate(
            relativeName: "יוסף",
            relation: .ben,
            motherName: "שרה",
            prayerType: .malade
        )
        // יוסף (4) + בן (2) + שרה (3) = 9 lettres, pas de נשמה
        XCTAssertEqual(seq.count, 9)
        XCTAssertFalse(seq.contains(where: { $0.source == .neshama }))
    }

    func test_generator_defunt_appendsNeshama() {
        let seq = LetterSequenceGenerator.generate(
            relativeName: "דוד",
            relation: .ben,
            motherName: "רחל",
            prayerType: .defunt
        )
        // דוד (3) + בן (2) + רחל (3) + נשמה (4) = 12
        XCTAssertEqual(seq.count, 12)
        let neshamaItems = seq.filter { $0.source == .neshama }
        XCTAssertEqual(neshamaItems.count, 4)
        XCTAssertEqual(neshamaItems.map(\.character), ["נ", "ש", "מ", "ה"])
    }

    func test_generator_ben_yossefBenSara() {
        let seq = LetterSequenceGenerator.generate(
            relativeName: "יוסף",
            relation: .ben,
            motherName: "שרה",
            prayerType: .malade
        )
        // ף final → פ
        let chars = seq.map(\.character)
        XCTAssertEqual(chars, ["י", "ו", "ס", "פ", "ב", "נ", "ש", "ר", "ה"])
        //                     [---proche---]  [lien]  [--mère--]
    }

    func test_generator_bat() {
        let seq = LetterSequenceGenerator.generate(
            relativeName: "רחל",
            relation: .bat,
            motherName: "לאה",
            prayerType: .malade
        )
        let chars = seq.map(\.character)
        XCTAssertEqual(chars, ["ר", "ח", "ל", "ב", "ת", "ל", "א", "ה"])
    }

    func test_generator_finalLettersAreMapped() {
        // « אברהם » contient ם final → doit être mappé en מ
        let seq = LetterSequenceGenerator.generate(
            relativeName: "אברהם",
            relation: .ben,
            motherName: "שרה",
            prayerType: .malade
        )
        let chars = seq.map(\.character)
        // אברהם → א ב ר ה מ (le ם final devient מ)
        XCTAssertEqual(chars[0..<5], ["א", "ב", "ר", "ה", "מ"])
    }

    func test_generator_sourcesAreCorrectlyAssigned() {
        let seq = LetterSequenceGenerator.generate(
            relativeName: "דן",
            relation: .ben,
            motherName: "שרה",
            prayerType: .defunt
        )
        // דן → ד נ (final ן → base נ)
        XCTAssertEqual(seq[0].source, .proche)
        XCTAssertEqual(seq[0].character, "ד")
        XCTAssertEqual(seq[1].source, .proche)
        XCTAssertEqual(seq[1].character, "נ")
        // בן
        XCTAssertEqual(seq[2].source, .lien)
        XCTAssertEqual(seq[3].source, .lien)
        // שרה
        XCTAssertEqual(seq[4].source, .mere)
        XCTAssertEqual(seq[5].source, .mere)
        XCTAssertEqual(seq[6].source, .mere)
        // נשמה
        XCTAssertEqual(seq[7].source, .neshama)
        XCTAssertEqual(seq[8].source, .neshama)
        XCTAssertEqual(seq[9].source, .neshama)
        XCTAssertEqual(seq[10].source, .neshama)
    }

    func test_generator_psalmLetterKey_equalsBaseCharacter() {
        // Garantit que la clé utilisée pour retrouver la section ne contient JAMAIS
        // une lettre finale (sinon section(forLetter:) retournerait nil).
        let seq = LetterSequenceGenerator.generate(
            relativeName: "צחק",  // ק n'est pas final, OK
            relation: .ben,
            motherName: "פנינה",
            prayerType: .malade
        )
        for item in seq {
            XCTAssertEqual(item.psalmLetterKey, item.character)
            XCTAssertFalse(["ך", "ם", "ן", "ף", "ץ"].contains(item.character))
        }
    }

    func test_generator_orderIndex_isSequential() {
        let seq = LetterSequenceGenerator.generate(
            relativeName: "דוד",
            relation: .ben,
            motherName: "רחל",
            prayerType: .defunt
        )
        for (i, item) in seq.enumerated() {
            XCTAssertEqual(item.orderIndex, i)
        }
    }

    func test_generator_makeTitle_format() {
        let title = LetterSequenceGenerator.makeTitle(
            prayerType: .malade,
            relativeName: "יוסף",
            relation: .ben,
            motherName: "שרה"
        )
        XCTAssertEqual(title, "Refoua Cheléma — יוסף בן שרה")
    }

    func test_generator_makeTitle_defunt() {
        let title = LetterSequenceGenerator.makeTitle(
            prayerType: .defunt,
            relativeName: "דוד",
            relation: .ben,
            motherName: "רחל"
        )
        XCTAssertEqual(title, "Lelouy Nichmat — דוד בן רחל")
    }
}

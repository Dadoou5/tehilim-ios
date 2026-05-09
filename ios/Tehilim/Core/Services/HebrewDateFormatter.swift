import Foundation

enum HebrewDateFormatter {
    struct DisplayDate {
        let dayOfWeek: String          // "Lundi"
        let transliterated: String     // "7 Iyar 5786"
        let hebrew: String             // "ז׳ באייר ה׳תשפ״ו"
    }

    static func formatted(_ date: Date = Date()) -> DisplayDate {
        let frFmt = DateFormatter()
        frFmt.locale = Locale(identifier: "fr_FR")
        frFmt.dateFormat = "EEEE"
        let day = frFmt.string(from: date).capitalized

        let latinFmt = DateFormatter()
        latinFmt.calendar = Calendar(identifier: .hebrew)
        latinFmt.locale = Locale(identifier: "en_US_POSIX")
        latinFmt.dateFormat = "d MMMM yyyy"
        let latin = latinFmt.string(from: date)

        let heFmt = DateFormatter()
        heFmt.calendar = Calendar(identifier: .hebrew)
        heFmt.locale = Locale(identifier: "he_IL")
        heFmt.dateStyle = .long
        let hebrew = heFmt.string(from: date)

        return DisplayDate(dayOfWeek: day, transliterated: latin, hebrew: hebrew)
    }
}

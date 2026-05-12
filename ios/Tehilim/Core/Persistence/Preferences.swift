import Foundation
import SwiftUI

enum AppTheme: String, Codable, CaseIterable, Identifiable {
    case system, light, dark
    var id: String { rawValue }

    var label: String {
        switch self {
        case .system: return String(localized: "Système")
        case .light:  return String(localized: "Clair")
        case .dark:   return String(localized: "Sombre")
        }
    }

    var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light:  return .light
        case .dark:   return .dark
        }
    }
}

enum TextSize: String, Codable, CaseIterable, Identifiable {
    case xs, s, m, l, xl
    var id: String { rawValue }

    /// Échelle relative — appliquée à la taille de base de chaque famille.
    var scale: CGFloat {
        switch self {
        case .xs: return 0.85
        case .s:  return 0.93
        case .m:  return 1.00
        case .l:  return 1.18
        case .xl: return 1.40
        }
    }

    var label: String {
        switch self {
        case .xs: return String(localized: "Très petit")
        case .s:  return String(localized: "Petit")
        case .m:  return String(localized: "Moyen")
        case .l:  return String(localized: "Grand")
        case .xl: return String(localized: "Très grand")
        }
    }
}

enum VerseNumberStyle: String, Codable, CaseIterable, Identifiable {
    case hebrew, arabic
    var id: String { rawValue }
    var label: String {
        self == .hebrew ? String(localized: "Hébreu") : String(localized: "Numérique")
    }
}

/// Mode d'affichage du texte principal du psaume.
/// - hebrew : caractères hébreux vocalisés (par défaut)
/// - phonetic : transcription latine sépharade (assistée, voir HebrewTransliterator)
enum TextMode: String, Codable, CaseIterable, Identifiable {
    case hebrew
    case phonetic
    var id: String { rawValue }

    var label: String {
        switch self {
        case .hebrew:   return String(localized: "Hébreu")
        case .phonetic: return String(localized: "Phonétique")
        }
    }
}

/// Façade simple sur UserDefaults via @AppStorage côté Views.
final class Preferences: ObservableObject {
    private let defaults: UserDefaults
    init(_ defaults: UserDefaults = .standard) { self.defaults = defaults }

    @AppStorage("pref.translation.fr")     var translationFR: Bool = false
    /// Langue de l'app : pilote l'UI (au prochain démarrage) et la traduction des Tehilim (instantané).
    @AppStorage("pref.app.language")       var appLanguage: AppLanguage = .system
    @AppStorage("pref.theme")              var theme: AppTheme = .system
    @AppStorage("pref.textSize.hebrew")    var textSizeHebrew: TextSize = .l
    @AppStorage("pref.textSize.fr")        var textSizeFR: TextSize = .m
    @AppStorage("pref.verseNumberStyle")   var verseNumberStyle: VerseNumberStyle = .hebrew
    @AppStorage("pref.textMode")           var textMode: TextMode = .hebrew
    /// Stocké dans le conteneur App Group partagé pour être accessible au widget.
    @AppStorage(AppGroup.Keys.dailyMode, store: AppGroup.userDefaults) var dailyMode: DailyMode = .monthly
    @AppStorage("pref.notif.enabled")      var notificationEnabled: Bool = false
    @AppStorage("pref.notif.hour")         var notificationHour: Int = 8
    @AppStorage("pref.notif.minute")       var notificationMinute: Int = 0
    @AppStorage("pref.lastReadPsalmId")    var lastReadPsalmId: Int = 0
    @AppStorage("pref.lastReadVerseId")    var lastReadVerseId: String = ""
    @AppStorage("pref.onboarding.done")    var onboardingCompleted: Bool = false
}

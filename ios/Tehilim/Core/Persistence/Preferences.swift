import Foundation
import SwiftUI
import Combine

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
///
/// V2.1.a — synchronisation iCloud des préférences via `NSUbiquitousKeyValueStore`.
/// Les 14 préférences sont sérialisées dans **un seul snapshot Codable** sous
/// la clé KVS `preferences.snapshot`. Quand une pref change localement, on push
/// le snapshot complet ; quand iCloud notifie un changement externe, on applique
/// le snapshot reçu sur UserDefaults — les @AppStorage propagent automatiquement
/// le changement à SwiftUI.
///
/// **Conflit** : last-write-wins sur l'ensemble du snapshot. Pour des changements
/// de préférences à rythme humain (toggle dans Réglages), c'est acceptable. Une
/// modification simultanée sur 2 devices résultera en la perte de l'une, jamais
/// d'une corruption partielle.
///
/// **Boucle de feedback** : `applyRemoteSnapshot()` écrit sur UserDefaults ce
/// qui re-fire `didChangeNotification` → `pushIfChanged()`. La déduplication
/// par égalité de snapshot (`lastPushedSnapshot == current`) coupe la boucle.
final class Preferences: ObservableObject {
    private let defaults: UserDefaults
    private var cancellable: AnyCancellable?
    private var udObserverToken: NSObjectProtocol?
    private var lastPushedSnapshot: PreferencesSnapshot?

    init(_ defaults: UserDefaults = .standard) {
        self.defaults = defaults
        bootstrapCloudSync()
        observeLocalChanges()
        observeCloudChanges()
    }

    deinit {
        if let token = udObserverToken {
            NotificationCenter.default.removeObserver(token)
        }
    }

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

    // MARK: - iCloud KVS sync (V2.1.a)

    static let kvsKey = "preferences.snapshot"

    /// Au démarrage : si iCloud a déjà un snapshot, on l'applique localement
    /// (le device adopte la config la plus récente sur le réseau). Sinon on
    /// pousse le snapshot local pour seed iCloud.
    private func bootstrapCloudSync() {
        if let cloud = iCloudKVS.shared.load(PreferencesSnapshot.self, forKey: Self.kvsKey) {
            applyRemoteSnapshot(cloud)
            // Snapshot effectivement appliqué (cf. note dans observeCloudChanges).
            lastPushedSnapshot = currentSnapshot()
        } else {
            let snap = currentSnapshot()
            iCloudKVS.shared.save(snap, forKey: Self.kvsKey)
            lastPushedSnapshot = snap
        }
    }

    /// Observe les écritures locales. `UserDefaults.didChangeNotification` est
    /// posté par TOUTES les instances UserDefaults du process (standard + App
    /// Group), donc on filtre côté `pushIfChanged` via l'égalité de snapshot.
    private func observeLocalChanges() {
        udObserverToken = NotificationCenter.default.addObserver(
            forName: UserDefaults.didChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.pushIfChanged()
        }
    }

    /// Observe les snapshots venus d'iCloud (autres devices).
    ///
    /// On mémorise ce qui a été **effectivement appliqué localement** (via
    /// `currentSnapshot()`), pas le snapshot reçu : si un champ d'enum porte
    /// un `rawValue` inconnu d'une version legacy (forward-compat), on ne le
    /// repushe pas vers iCloud — préservant la valeur des devices à jour.
    private func observeCloudChanges() {
        cancellable = iCloudKVS.shared.externalChange.sink { [weak self] keys in
            guard let self, keys.contains(Self.kvsKey) else { return }
            if let snap = iCloudKVS.shared.load(PreferencesSnapshot.self, forKey: Self.kvsKey) {
                self.applyRemoteSnapshot(snap)
                self.lastPushedSnapshot = self.currentSnapshot()
            }
        }
    }

    private func pushIfChanged() {
        let now = currentSnapshot()
        guard now != lastPushedSnapshot else { return }
        iCloudKVS.shared.save(now, forKey: Self.kvsKey)
        lastPushedSnapshot = now
    }

    private func currentSnapshot() -> PreferencesSnapshot {
        PreferencesSnapshot(
            translationFR: translationFR,
            appLanguage: appLanguage.rawValue,
            theme: theme.rawValue,
            textSizeHebrew: textSizeHebrew.rawValue,
            textSizeFR: textSizeFR.rawValue,
            verseNumberStyle: verseNumberStyle.rawValue,
            textMode: textMode.rawValue,
            dailyMode: dailyMode.rawValue,
            notificationEnabled: notificationEnabled,
            notificationHour: notificationHour,
            notificationMinute: notificationMinute,
            lastReadPsalmId: lastReadPsalmId,
            lastReadVerseId: lastReadVerseId,
            onboardingCompleted: onboardingCompleted
        )
    }

    /// Écrit chaque champ du snapshot dans UserDefaults via les @AppStorage —
    /// SwiftUI met à jour les Views automatiquement. `dailyMode` est routé vers
    /// le store App Group pour rester accessible au widget.
    ///
    /// Les enums avec `rawValue` invalide sont ignorés (cas d'une version future
    /// qui ajouterait des cases : on ne casse pas le device legacy).
    private func applyRemoteSnapshot(_ snap: PreferencesSnapshot) {
        translationFR = snap.translationFR
        if let v = AppLanguage(rawValue: snap.appLanguage) { appLanguage = v }
        if let v = AppTheme(rawValue: snap.theme) { theme = v }
        if let v = TextSize(rawValue: snap.textSizeHebrew) { textSizeHebrew = v }
        if let v = TextSize(rawValue: snap.textSizeFR) { textSizeFR = v }
        if let v = VerseNumberStyle(rawValue: snap.verseNumberStyle) { verseNumberStyle = v }
        if let v = TextMode(rawValue: snap.textMode) { textMode = v }
        if let v = DailyMode(rawValue: snap.dailyMode) { dailyMode = v }
        notificationEnabled = snap.notificationEnabled
        notificationHour = snap.notificationHour
        notificationMinute = snap.notificationMinute
        lastReadPsalmId = snap.lastReadPsalmId
        lastReadVerseId = snap.lastReadVerseId
        onboardingCompleted = snap.onboardingCompleted
    }
}

/// Snapshot Codable de l'état complet des préférences pour transit iCloud.
///
/// Enums sérialisés via leurs `rawValue` (String) — robuste à l'ajout futur de
/// cases et lisible dans le store. L'`Equatable` synthétisé permet la
/// déduplication des push (cf. `Preferences.pushIfChanged`).
struct PreferencesSnapshot: Codable, Equatable {
    let translationFR: Bool
    let appLanguage: String
    let theme: String
    let textSizeHebrew: String
    let textSizeFR: String
    let verseNumberStyle: String
    let textMode: String
    let dailyMode: String
    let notificationEnabled: Bool
    let notificationHour: Int
    let notificationMinute: Int
    let lastReadPsalmId: Int
    let lastReadVerseId: String
    let onboardingCompleted: Bool
}

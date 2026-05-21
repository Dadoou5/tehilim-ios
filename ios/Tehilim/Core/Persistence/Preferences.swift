import Foundation
import SwiftUI
import Combine

enum AppTheme: String, Codable, CaseIterable, Identifiable {
    case system, light, dark
    var id: String { rawValue }

    var label: String {
        switch self {
        case .system: return L("Système")
        case .light:  return L("Clair")
        case .dark:   return L("Sombre")
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
        case .xs: return L("Très petit")
        case .s:  return L("Petit")
        case .m:  return L("Moyen")
        case .l:  return L("Grand")
        case .xl: return L("Très grand")
        }
    }
}

enum VerseNumberStyle: String, Codable, CaseIterable, Identifiable {
    case hebrew, arabic
    var id: String { rawValue }
    var label: String {
        self == .hebrew ? L("Hébreu") : L("Numérique")
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
        case .hebrew:   return L("Hébreu")
        case .phonetic: return L("Phonétique")
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

    /// V2.1.a — garde-fou : le bootstrap iCloud et les observers ne tournent
    /// qu'une seule fois par process. Les Views créent leur propre instance
    /// `Preferences()` via @StateObject, mais seule la première (celle de
    /// `AppContainer`) installe la machinerie de sync. Sans ce garde-fou, la
    /// 2e/3e instance ré-exécuterait `applyRemoteSnapshot` et pourrait écraser
    /// des valeurs récentes (ex. `onboardingCompleted=true` après que
    /// l'utilisateur a terminé l'onboarding) avec un snapshot KVS antérieur.
    private static var didInstallCloudSync = false

    init(_ defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if !Self.didInstallCloudSync {
            Self.didInstallCloudSync = true
            bootstrapCloudSync()
            observeLocalChanges()
            observeCloudChanges()
        }
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
    ///
    /// **Défense récursion** : Apple poste `didChangeNotification` de façon
    /// **synchrone** sur la thread d'écriture, même avec `queue: .main` quand
    /// on est déjà sur main. On force l'exécution sur un prochain run loop
    /// via `DispatchQueue.main.async` pour casser la pile et empêcher la
    /// récursion infinie sur le `local.set()` dans `iCloudKVS.save()`.
    private func observeLocalChanges() {
        udObserverToken = NotificationCenter.default.addObserver(
            forName: UserDefaults.didChangeNotification,
            object: nil,
            queue: nil
        ) { [weak self] _ in
            DispatchQueue.main.async {
                self?.pushIfChanged()
            }
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
        // **Avant save()** : si une notif synchrone re-déclenchait pushIfChanged
        // pendant `iCloudKVS.shared.save`, elle verrait now == lastPushed et
        // ressortirait immédiatement — pas de récursion. Combiné avec le
        // dispatch async de l'observer, on est doublement protégé.
        lastPushedSnapshot = now
        iCloudKVS.shared.save(now, forKey: Self.kvsKey)
    }

    private func currentSnapshot() -> PreferencesSnapshot {
        // V2.1.a — `onboardingCompleted`, `lastReadPsalmId` et `lastReadVerseId`
        // SONT VOLONTAIREMENT EXCLUS du snapshot iCloud :
        // - onboardingCompleted = flag par appareil (« cet appareil a vu
        //   l'intro »), pas une préférence user → un nouveau device doit
        //   refaire l'onboarding.
        // - lastReadPsalmId / lastReadVerseId = session locale, ne doit pas
        //   être écrasé par un autre device au milieu d'une lecture.
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
            notificationMinute: notificationMinute
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
        // onboardingCompleted / lastReadPsalmId / lastReadVerseId :
        // intentionnellement non synchronisés (voir currentSnapshot).
    }
}

/// Snapshot Codable des préférences utilisateur réellement synchronisables
/// entre appareils via iCloud KVS.
///
/// Exclus volontairement (cf. `Preferences.currentSnapshot`) :
/// - `onboardingCompleted` (flag par appareil, pas une pref)
/// - `lastReadPsalmId` / `lastReadVerseId` (session locale)
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
}

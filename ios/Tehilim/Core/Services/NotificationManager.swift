import Foundation
import UserNotifications
import UIKit
import os.log

/// V1.10.7 — Représente un rappel d'azcara actuellement programmé côté
/// iOS (= présent dans `UNUserNotificationCenter.pendingNotificationRequests`).
/// Utilisé par l'UI de diagnostic dans le détail d'un Lelouy Nichmat.
struct PendingMemorialReminder: Identifiable, Hashable {
    enum Kind: Hashable {
        case sevenDays  // J-7 (« Azcara dans 7 jours »)
        case sameDay    // Jour J à 9h locale
    }
    let id: String          // = identifier de la UNNotificationRequest
    let kind: Kind
    let triggerDate: Date   // résolu via UNCalendarNotificationTrigger.nextTriggerDate()
}

/// Gère le rappel quotidien des Tehilim du jour : permission, planification, deep link.
@MainActor
final class NotificationManager: NSObject, ObservableObject {
    static let shared = NotificationManager()

    nonisolated static let dailyReminderId = "tehilim.daily.reminder"
    nonisolated static let routeKey = "route"
    nonisolated static let routeDailyValue = "daily"

    nonisolated private static let log = Logger(subsystem: "com.david.tehilim", category: "Notifications")

    @Published private(set) var permission: UNAuthorizationStatus = .notDetermined
    /// Mis à jour quand l'utilisateur tape une notification. Observé par RootTabView.
    @Published var pendingRoute: TabRouter.Tab? = nil

    private override init() {
        super.init()
        UNUserNotificationCenter.current().delegate = self
        Self.log.info("Delegate set on UNUserNotificationCenter")
        Task { await refreshPermission() }
    }

    // MARK: - Permission

    func refreshPermission() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        self.permission = settings.authorizationStatus
        Self.log.info("Permission refreshed: \(settings.authorizationStatus.rawValue, privacy: .public)")
    }

    func requestPermission() async -> Bool {
        do {
            let granted = try await UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .sound, .badge])
            await refreshPermission()
            Self.log.info("Permission requested → granted=\(granted, privacy: .public)")
            return granted
        } catch {
            Self.log.error("Permission request failed: \(error.localizedDescription, privacy: .public)")
            return false
        }
    }

    // MARK: - Schedule

    func scheduleDailyReminder(hour: Int, minute: Int) async {
        await cancelDailyReminder()

        let content = UNMutableNotificationContent()
        content.title = L("Tehilim du jour")
        content.body  = L("C'est le moment de lire tes Tehilim du jour.")
        content.sound = .default
        content.userInfo = [Self.routeKey: Self.routeDailyValue]

        var components = DateComponents()
        components.hour = hour
        components.minute = minute
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: true)

        let request = UNNotificationRequest(
            identifier: Self.dailyReminderId,
            content: content,
            trigger: trigger
        )

        do {
            try await UNUserNotificationCenter.current().add(request)
            Self.log.info("Scheduled daily reminder at \(hour):\(String(format: "%02d", minute))")
        } catch {
            Self.log.error("Failed to schedule reminder: \(error.localizedDescription, privacy: .public)")
        }
    }

    func cancelDailyReminder() async {
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: [Self.dailyReminderId])
        Self.log.info("Cancelled daily reminder")
    }

    // MARK: - Memorial reminders (V1.10.7 — Commémoration)

    /// Identifiants de notification pour les rappels d'azcara — préfixe
    /// avec l'id du SavedPrayerIntent pour pouvoir annuler ciblé.
    static func memorialIds(intentId: UUID) -> (sevenDays: String, sameDay: String) {
        (
            "tehilim.memorial.\(intentId.uuidString).j7",
            "tehilim.memorial.\(intentId.uuidString).day"
        )
    }

    /// Annule tous les rappels d'azcara pour un intent donné. Appelé quand
    /// l'utilisateur désactive les rappels, modifie la date, ou supprime
    /// le Lelouy Nichmat.
    func cancelMemorialReminders(intentId: UUID) async {
        let ids = Self.memorialIds(intentId: intentId)
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: [ids.sevenDays, ids.sameDay])
        Self.log.info("Cancelled memorial reminders for \(intentId.uuidString, privacy: .public)")
    }

    /// V1.10.7 — Diagnostic : retourne les rappels d'azcara actuellement
    /// programmés pour un intent. Permet à l'UI d'afficher « Rappels
    /// programmés : J-7 le 17 mai 12:00, jour J le 24 mai 09:00 » et
    /// donc de vérifier visuellement que la chaîne notif fonctionne
    /// (sans avoir à attendre le déclenchement réel ou bricoler l'horloge).
    func pendingMemorialReminders(intentId: UUID) async -> [PendingMemorialReminder] {
        let ids = Self.memorialIds(intentId: intentId)
        let requests = await UNUserNotificationCenter.current().pendingNotificationRequests()
        return requests.compactMap { req -> PendingMemorialReminder? in
            let kind: PendingMemorialReminder.Kind
            switch req.identifier {
            case ids.sevenDays: kind = .sevenDays
            case ids.sameDay: kind = .sameDay
            default: return nil
            }
            guard let trigger = req.trigger as? UNCalendarNotificationTrigger,
                  let nextFire = trigger.nextTriggerDate() else {
                return nil
            }
            return PendingMemorialReminder(id: req.identifier, kind: kind, triggerDate: nextFire)
        }
        .sorted { $0.triggerDate < $1.triggerDate }
    }

    /// Reprogramme (annule puis re-planifie) les rappels d'azcara pour
    /// un intent. Calcule la prochaine date azcara civile à partir de la
    /// date du décès, puis pose les UNCalendarNotificationTrigger.
    ///
    /// **Conditions** (gérées par le caller) : intent.remindersEnabled +
    /// au moins un toggle activé + date du décès présente.
    func rescheduleMemorialReminders(for intent: SavedPrayerIntent) async {
        await cancelMemorialReminders(intentId: intent.id)

        guard intent.remindersEnabled,
              intent.notifySevenDaysBefore || intent.notifySameDay,
              let death = intent.civilDateOfDeath,
              let nextAzcara = MemorialCalculator.nextYahrzeit(deathCivil: death) else {
            return
        }

        let ids = Self.memorialIds(intentId: intent.id)
        let cal = Calendar.current

        if intent.notifySevenDaysBefore,
           let triggerDate = cal.date(byAdding: .day, value: -7, to: nextAzcara),
           triggerDate > Date() {
            await schedule(
                id: ids.sevenDays,
                title: L("Azcara dans 7 jours"),
                body: bodyFor(intent: intent, daysOffset: 7),
                date: triggerDate
            )
        }
        if intent.notifySameDay, nextAzcara > Date() {
            // 9h locale le jour J pour ne pas réveiller la nuit.
            var dc = cal.dateComponents([.year, .month, .day], from: nextAzcara)
            dc.hour = 9
            if let sameDay = cal.date(from: dc), sameDay > Date() {
                await schedule(
                    id: ids.sameDay,
                    title: L("Azcara aujourd'hui"),
                    body: bodyFor(intent: intent, daysOffset: 0),
                    date: sameDay
                )
            }
        }
    }

    private func bodyFor(intent: SavedPrayerIntent, daysOffset: Int) -> String {
        let subject = intent.hebrewSubject
        if daysOffset == 0 {
            return String(format: L("Azcara de %@."), subject)
        }
        return String(
            format: L("Azcara de %@ dans %lld jours."),
            subject, daysOffset
        )
    }

    private func schedule(id: String, title: String, body: String, date: Date) async {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default

        let comps = Calendar.current.dateComponents(
            [.year, .month, .day, .hour, .minute],
            from: date
        )
        let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)
        let request = UNNotificationRequest(identifier: id, content: content, trigger: trigger)
        do {
            try await UNUserNotificationCenter.current().add(request)
            Self.log.info("Scheduled memorial reminder \(id, privacy: .public) at \(date, privacy: .public)")
        } catch {
            Self.log.error("Failed to schedule memorial reminder: \(error.localizedDescription, privacy: .public)")
        }
    }

    // MARK: - Open iOS Settings

    func openSystemSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    }
}

// MARK: - UNUserNotificationCenterDelegate

extension NotificationManager: UNUserNotificationCenterDelegate {

    /// Affiche la bannière même quand l'app est au premier plan.
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        Self.log.info("willPresent: \(notification.request.identifier, privacy: .public)")
        completionHandler([.banner, .sound, .list])
    }

    /// Tap sur la notification → enregistre la route à ouvrir.
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        let route = userInfo[Self.routeKey] as? String
        // Push de chaîne (APNs) : `chainId` en clé custom → ouvre l'écran chaîne.
        let chainId = userInfo["chainId"] as? String
        Self.log.info("didReceive: route=\(route ?? "nil", privacy: .public) chainId=\(chainId ?? "nil", privacy: .public)")
        Task { @MainActor in
            if let chainId, !chainId.isEmpty {
                AppContainer.shared.pendingChainOpen = chainId
                Self.log.info("pendingChainOpen set from push")
            } else if route == Self.routeDailyValue {
                self.pendingRoute = .daily
                Self.log.info("pendingRoute set to .daily")
            }
            completionHandler()
        }
    }
}

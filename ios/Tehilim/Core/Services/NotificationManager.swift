import Foundation
import UserNotifications
import UIKit
import os.log

/// Gère le rappel quotidien des Tehilim du jour : permission, planification, deep link.
@MainActor
final class NotificationManager: NSObject, ObservableObject {
    static let shared = NotificationManager()

    static let dailyReminderId = "tehilim.daily.reminder"
    static let routeKey = "route"
    static let routeDailyValue = "daily"

    private static let log = Logger(subsystem: "com.david.tehilim", category: "Notifications")

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
        content.title = "Tehilim du jour"
        content.body  = "C'est le moment de lire tes Tehilim du jour."
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
        Self.log.info("didReceive: route=\(route ?? "nil", privacy: .public)")
        Task { @MainActor in
            if route == Self.routeDailyValue {
                self.pendingRoute = .daily
                Self.log.info("pendingRoute set to .daily")
            }
            completionHandler()
        }
    }
}

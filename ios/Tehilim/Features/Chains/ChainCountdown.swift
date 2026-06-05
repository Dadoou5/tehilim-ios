import Foundation

/// Compte à rebours partagé des chaînes. Format `HH:MM:SS` (préfixé des jours si
/// besoin) → les **secondes défilent** chaque tick, contrairement à un affichage
/// « 5 h 23 min » qui semblait figé.
enum ChainCountdown {
    static func format(seconds: Int) -> String {
        let secs = max(0, seconds)
        let d = secs / 86400, h = (secs % 86400) / 3600, m = (secs % 3600) / 60, s = secs % 60
        let hh = String(format: "%02d", h), mm = String(format: "%02d", m), ss = String(format: "%02d", s)
        let dU = AppLocale.code == "en" ? "d" : "j"
        return d > 0 ? "\(d)\(dU) \(hh):\(mm):\(ss)" : "\(hh):\(mm):\(ss)"
    }
}

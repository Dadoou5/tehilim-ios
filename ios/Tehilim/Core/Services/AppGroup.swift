import Foundation
import os

/// Identifiant et accès au conteneur partagé entre l'app et son extension widget.
/// Permet aux deux processus de lire/écrire sur le même `UserDefaults`.
///
/// **Déploiement** : l'App Group nécessite l'enregistrement sur Apple Developer
/// portal (membership payant) pour fonctionner sur device. Sur simulateur, il
/// fonctionne sans enregistrement.
///
/// En free provisioning (ou si l'App Group n'est pas activé dans le portail),
/// iOS journalise une warning CFPrefsPlistSource à chaque accès. On la prévient
/// en probant l'accessibilité au démarrage : si le roundtrip échoue, on bascule
/// silencieusement sur `UserDefaults.standard` (le widget ne sera pas synchro
/// mais l'app fonctionne normalement).
enum AppGroup {
    static let id = "group.com.david.tehilim"

    private static let log = Logger(subsystem: "com.david.tehilim", category: "AppGroup")

    /// `UserDefaults` partagé entre app et widget, ou `.standard` si l'App
    /// Group n'est pas accessible (free provisioning, capability non activée).
    ///
    /// Double garde :
    /// 1. `FileManager.containerURL` — vérifie l'existence du conteneur sans
    ///    toucher cfprefsd (évite le warning CFPrefsPlistSource).
    /// 2. Probe roundtrip — détecte le cas simulateur où le conteneur existe
    ///    côté filesystem mais les écritures sont silencieusement ignorées.
    static let userDefaults: UserDefaults = {
        // Étape 1 : conteneur partagé présent ? (FileManager, pas cfprefsd)
        guard FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: id) != nil else {
            log.notice("App Group \(id, privacy: .public) : conteneur absent — UserDefaults.standard. Enregistre l'App Group sur developer.apple.com pour activer la synchro widget.")
            return .standard
        }
        // Étape 2 : probe roundtrip pour détecter une initialisation sans accès réel
        guard let suite = UserDefaults(suiteName: id), isAccessible(suite) else {
            log.notice("App Group \(id, privacy: .public) : roundtrip échoué — UserDefaults.standard.")
            return .standard
        }
        return suite
    }()

    /// Probe : écrit une clé sentinelle, la relit, et vérifie le roundtrip.
    /// Si l'écriture est silencieusement ignorée par iOS (cas typique de l'App
    /// Group non enregistré), on bascule.
    private static func isAccessible(_ defaults: UserDefaults) -> Bool {
        let probeKey = "appgroup.probe.\(UUID().uuidString.prefix(8))"
        let probeValue = "ok"
        defaults.set(probeValue, forKey: probeKey)
        let readBack = defaults.string(forKey: probeKey)
        defaults.removeObject(forKey: probeKey)
        return readBack == probeValue
    }

    enum Keys {
        static let dailyMode = "pref.dailyMode"
    }
}

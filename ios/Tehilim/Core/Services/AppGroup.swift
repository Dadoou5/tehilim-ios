import Foundation

/// Identifiant et accès au conteneur partagé entre l'app et son extension widget.
/// Permet aux deux processus de lire/écrire sur le même `UserDefaults`.
///
/// **Déploiement** : l'App Group nécessite l'enregistrement sur Apple Developer
/// portal (membership payant) pour fonctionner sur device. Sur simulateur,
/// il fonctionne sans enregistrement.
enum AppGroup {
    static let id = "group.com.david.tehilim"

    /// `UserDefaults` partagé. Fallback sur `.standard` si la suite est inaccessible
    /// (cas de free provisioning sur device sans App Group enregistré).
    static let userDefaults: UserDefaults = {
        UserDefaults(suiteName: id) ?? .standard
    }()

    enum Keys {
        static let dailyMode = "pref.dailyMode"
    }
}

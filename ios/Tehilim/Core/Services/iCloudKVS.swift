import Foundation
import Combine

/// Façade autour de `NSUbiquitousKeyValueStore` pour synchroniser des données
/// utilisateur entre les appareils via iCloud.
///
/// **Limites Apple** : 1 MB total, 1024 clés max, 1 MB max par valeur, 0.5 MB max
/// par chaîne UTF-8. Largement suffisant pour des favoris (liste d'Int) et des
/// Lelouy Nichmat (JSON compact d'objets Codable).
///
/// **Fallback** : si iCloud n'est pas disponible (utilisateur non connecté à
/// iCloud, simulateur sans Apple ID, mode hors-ligne sans cache), on retombe
/// silencieusement sur `UserDefaults.standard` — l'app continue de fonctionner,
/// juste sans sync.
///
/// **Notifications** : on observe `NSUbiquitousKeyValueStore.didChangeExternallyNotification`
/// pour mettre à jour les stores quand iCloud nous notifie d'un changement
/// venu d'un autre appareil.
final class iCloudKVS {

    static let shared = iCloudKVS()

    private let cloud: NSUbiquitousKeyValueStore
    private let local: UserDefaults

    /// Émet la liste des clés modifiées suite à une notification iCloud externe.
    let externalChange = PassthroughSubject<[String], Never>()

    private var notificationToken: NSObjectProtocol?

    private init() {
        self.cloud = .default
        self.local = .standard

        // Force une synchronisation initiale au démarrage de l'app.
        // Apple recommande d'appeler `synchronize` après chaque batch d'écriture
        // et au lancement pour pull les changements distants.
        _ = cloud.synchronize()

        // Écoute les changements venant d'iCloud (autres devices).
        notificationToken = NotificationCenter.default.addObserver(
            forName: NSUbiquitousKeyValueStore.didChangeExternallyNotification,
            object: cloud,
            queue: .main
        ) { [weak self] notification in
            guard let self else { return }
            let keys = notification.userInfo?[NSUbiquitousKeyValueStoreChangedKeysKey] as? [String] ?? []
            // Réplique sur le store local pour le offline.
            for key in keys {
                self.mirrorCloudToLocal(key)
            }
            self.externalChange.send(keys)
        }
    }

    deinit {
        if let token = notificationToken {
            NotificationCenter.default.removeObserver(token)
        }
    }

    // MARK: - Données (Codable JSON)

    /// Lit un objet Codable depuis iCloud (ou local si iCloud indisponible).
    func load<T: Decodable>(_ type: T.Type, forKey key: String) -> T? {
        if let data = cloud.data(forKey: key) {
            return decode(type, data: data)
        }
        if let data = local.data(forKey: key) {
            return decode(type, data: data)
        }
        return nil
    }

    /// Sauvegarde un objet Codable sur iCloud + miroir local.
    func save<T: Encodable>(_ value: T, forKey key: String) {
        guard let data = try? JSONEncoder().encode(value) else { return }
        cloud.set(data, forKey: key)
        local.set(data, forKey: key)
        _ = cloud.synchronize()
    }

    /// Lit un tableau d'Int (pratique pour les favoris).
    func loadIntArray(forKey key: String) -> [Int]? {
        if let array = cloud.array(forKey: key) as? [Int] {
            return array
        }
        if let array = local.array(forKey: key) as? [Int] {
            return array
        }
        return nil
    }

    /// Sauvegarde un tableau d'Int sur iCloud + miroir local.
    func saveIntArray(_ array: [Int], forKey key: String) {
        cloud.set(array, forKey: key)
        local.set(array, forKey: key)
        _ = cloud.synchronize()
    }

    // MARK: - Migration depuis l'ancien storage local

    /// Si la clé n'existe pas sur iCloud mais existe dans l'ancien storage
    /// (UserDefaults legacy ou fichier JSON), on migre.
    ///
    /// Utilisé par les stores au premier lancement V1.10.5+ pour ne pas perdre
    /// les favoris et Lelouy Nichmat précédemment stockés localement.
    func migrateIfNeeded(key: String, legacyData: Data?) {
        guard cloud.data(forKey: key) == nil, let data = legacyData else { return }
        cloud.set(data, forKey: key)
        local.set(data, forKey: key)
        _ = cloud.synchronize()
    }

    func migrateIntArrayIfNeeded(key: String, legacyArray: [Int]?) {
        guard cloud.array(forKey: key) == nil, let array = legacyArray, !array.isEmpty else { return }
        cloud.set(array, forKey: key)
        local.set(array, forKey: key)
        _ = cloud.synchronize()
    }

    // MARK: - Internals

    private func decode<T: Decodable>(_ type: T.Type, data: Data) -> T? {
        do {
            return try JSONDecoder().decode(type, from: data)
        } catch {
            print("iCloudKVS: decode failed for \(type) — \(error)")
            return nil
        }
    }

    /// Copie la valeur d'iCloud vers UserDefaults.standard pour le mode offline.
    private func mirrorCloudToLocal(_ key: String) {
        if let data = cloud.data(forKey: key) {
            local.set(data, forKey: key)
        } else if let array = cloud.array(forKey: key) {
            local.set(array, forKey: key)
        }
    }
}

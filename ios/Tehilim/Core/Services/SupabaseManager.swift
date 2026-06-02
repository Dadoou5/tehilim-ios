import Foundation
import Supabase

/// Fournit le **client Supabase** unique de l'app, créé une seule fois à partir
/// de `Supabase-Info.plist` (clés `SUPABASE_URL` + `SUPABASE_ANON_KEY`).
///
/// Si le plist est absent du bundle (clone sans config, CI, contributeur
/// externe) → `client == nil` et la feature « Chaîne de Tehilim » reste
/// **désactivée** : l'app demeure 100 % locale. C'est exactement la garantie
/// qu'offrait la config Firebase auparavant (build & run verts sans secret).
///
/// La clé anon est *publique par conception* (protégée par la RLS Postgres) ;
/// on la garde toutefois hors du dépôt (gitignore) par convention, comme l'était
/// `GoogleService-Info.plist`.
enum SupabaseConfig {
    static func load() -> (url: URL, anonKey: String)? {
        guard let path = Bundle.main.path(forResource: "Supabase-Info", ofType: "plist"),
              let dict = NSDictionary(contentsOfFile: path),
              let urlString = dict["SUPABASE_URL"] as? String, !urlString.isEmpty,
              let anonKey = dict["SUPABASE_ANON_KEY"] as? String, !anonKey.isEmpty,
              let url = URL(string: urlString)
        else { return nil }
        return (url, anonKey)
    }
}

final class SupabaseManager {
    static let shared = SupabaseManager()

    /// `nil` quand la config est absente → feature chaîne désactivée.
    let client: SupabaseClient?

    private init() {
        if let cfg = SupabaseConfig.load() {
            client = SupabaseClient(supabaseURL: cfg.url, supabaseKey: cfg.anonKey)
        } else {
            client = nil
        }
    }
}

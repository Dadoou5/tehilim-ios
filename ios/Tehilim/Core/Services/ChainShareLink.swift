import Foundation

/// Encodage / décodage du lien de partage d'une **chaîne de Tehilim**.
///
/// Mirror de `PrayerShareLink`, mais le payload est minimal : seul l'**id de
/// chaîne** circule (le reste est lu depuis Supabase à l'ouverture). Le lien
/// est une page de redirection `https://…/c/?id=…` (cliquable dans WhatsApp),
/// qui rouvre `tehilim://chain?id=…`. Universal Link sur `/c/` (cf. AASA).
enum ChainShareLink {

    static let scheme = "tehilim"
    static let host = "chain"

    /// Page de redirection https (repo Pages séparé) — cliquable partout.
    static let webBaseURL = "https://tehilimapp.com/c/"

    // MARK: - Encodage

    /// Lien de partage `https://…/c/?id=<chainId>` représentant la chaîne.
    static func url(forChainId chainId: String) -> URL? {
        guard var comps = URLComponents(string: webBaseURL) else { return nil }
        comps.queryItems = [URLQueryItem(name: "id", value: chainId)]
        return comps.url
    }

    /// Message texte prêt à partager (WhatsApp) : description de la chaîne +
    /// le lien d'ouverture. Le destinataire tape le lien → ouvre l'app sur la
    /// chaîne et peut rejoindre / sélectionner ses Tehilim.
    static func shareMessage(for chain: TehilimChain) -> String {
        let fr = AppLocale.code == "fr"   // hébreu → message EN
        let link = url(forChainId: chain.id)?.absoluteString ?? ""
        var lines: [String] = []
        lines.append((fr ? "Chaîne de Tehilim — " : "Tehilim chain — ") + chain.subjectLine)
        lines.append("")
        lines.append(fr
            ? "Rejoins la chaîne et choisis les Tehilim que tu liras :"
            : "Join the chain and pick the Tehilim you'll read:")
        lines.append(link)
        return lines.joined(separator: "\n")
    }

    // MARK: - Décodage

    /// True si l'URL ouvre une chaîne : `tehilim://chain` OU Universal Link
    /// `https://…/c/…`. `URL.path` retire le « / » final → on accepte « /c »
    /// exact ET « /c/… » (sans matcher « /privacy » ni « /p »).
    static func isChainLink(_ url: URL) -> Bool {
        if url.scheme == scheme && url.host == host { return true }
        if url.scheme == "https" && (url.path == "/c" || url.path.hasPrefix("/c/")) { return true }
        return false
    }

    /// Extrait l'id de chaîne d'un lien (`?id=…`). Nil si absent/invalide.
    static func chainId(from url: URL) -> String? {
        guard isChainLink(url),
              let comps = URLComponents(url: url, resolvingAgainstBaseURL: false)
        else { return nil }
        let id = comps.queryItems?.first(where: { $0.name == "id" })?.value?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard let id, !id.isEmpty else { return nil }
        return id
    }
}

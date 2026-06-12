import Foundation

/// Encodage / décodage d'une prière partageable via lien `tehilim://prayer`.
///
/// Le lien est volontairement **minimal** : seuls les champs « source » sont
/// transmis (type, prénom du proche, lien de parenté, prénom de la mère,
/// date du décès optionnelle). La séquence de lettres (`generatedLetters`)
/// et la date hébraïque (`hebrewDateOfDeath`) sont **recalculées à l'import**
/// — elles dérivent déterministiquement de ces champs, inutile de les
/// transporter (URL plus courte, robuste aux évolutions du générateur).
///
/// Format : `tehilim://prayer?v=1&type=<malade|defunt>&name=<enc>&rel=<ben|bat>&mother=<enc>&death=<yyyy-MM-dd>`
///
/// Le schéma `tehilim://` est déjà enregistré (CFBundleURLSchemes) et géré
/// par `RootTabView.handleDeepLink`.
enum PrayerShareLink {

    static let scheme = "tehilim"
    static let host = "prayer"
    /// Version du format de payload — permet d'évoluer sans casser les liens
    /// déjà partagés.
    static let version = "1"

    /// Page de redirection https (GitHub Pages) — un lien `https://` est
    /// cliquable dans Mail / WhatsApp / SMS (contrairement à `tehilim://`).
    /// La page lit les mêmes paramètres et rouvre `tehilim://prayer?...`.
    static let webBaseURL = "https://tehilimapp.com/p/"

    /// `yyyy-MM-dd` en calendrier grégorien + locale POSIX : format stable,
    /// identique côté Android, indépendant de la locale de l'appareil.
    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = .current
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    // MARK: - Encodage

    /// Construit le lien de partage `https://…/p/?...` (page de redirection)
    /// représentant l'intent. Cliquable dans tous les messageries.
    static func url(for intent: SavedPrayerIntent) -> URL? {
        guard var comps = URLComponents(string: webBaseURL) else { return nil }
        var items: [URLQueryItem] = [
            URLQueryItem(name: "v", value: version),
            URLQueryItem(name: "type", value: intent.prayerType.rawValue),
            URLQueryItem(name: "name", value: intent.relativeFirstName),
            URLQueryItem(name: "rel", value: intent.relationType.rawValue),
            URLQueryItem(name: "mother", value: intent.motherFirstName)
        ]
        if let death = intent.civilDateOfDeath {
            items.append(URLQueryItem(name: "death", value: dateFormatter.string(from: death)))
        }
        comps.queryItems = items
        return comps.url
    }

    /// Message texte prêt à partager (SMS / WhatsApp) : description lisible,
    /// date du décès + prochaine azcara (dans la langue de l'expéditeur),
    /// puis le lien d'import. Taper le lien ouvre l'app et propose l'import.
    static func shareMessage(for intent: SavedPrayerIntent) -> String {
        let fr = AppLocale.code == "fr"   // hébreu → message EN
        let link = url(for: intent)?.absoluteString ?? ""
        let df = DateFormatter()
        df.locale = AppLocale.locale
        df.dateStyle = .long

        var lines: [String] = ["\(intent.prayerType.saveActionTitle) — \(intent.hebrewSubject)"]
        if let death = intent.civilDateOfDeath {
            lines.append((fr ? "Date de décès : " : "Date of death: ") + df.string(from: death))
            if let next = MemorialCalculator.nextYahrzeit(deathCivil: death) {
                lines.append((fr ? "Prochaine azcara : " : "Next azcara: ") + df.string(from: next)
                             + (fr ? " (commence la veille au soir)" : " (begins the previous evening)"))
            }
        }
        lines.append("")
        lines.append(fr ? "Ouvre cette prière dans Tehilim :" : "Open this prayer in Tehilim:")
        lines.append(link)
        return lines.joined(separator: "\n")
    }

    // MARK: - Décodage

    /// Payload décodé depuis un lien partagé.
    /// `Identifiable` pour piloter une présentation `.sheet(item:)`.
    struct Payload: Identifiable {
        let id = UUID()
        let prayerType: PrayerType
        let relativeFirstName: String
        let relationType: RelationType
        let motherFirstName: String
        let civilDateOfDeath: Date?

        var hebrewSubject: String {
            "\(relativeFirstName) \(relationType.hebrew) \(motherFirstName)"
        }
    }

    /// True si l'URL est un lien de prière géré : schéma custom
    /// `tehilim://prayer` OU Universal Link `https://…/p/…`.
    static func isPrayerLink(_ url: URL) -> Bool {
        if url.scheme == scheme && url.host == host { return true }
        // `URL.path` retire le « / » final : `https://…/p/?...` donne « /p ».
        // On accepte donc « /p » exact ET « /p/… », sans matcher « /privacy ».
        if url.scheme == "https" && (url.path == "/p" || url.path.hasPrefix("/p/")) { return true }
        return false
    }

    /// Parse un lien de prière (`tehilim://prayer?...` ou `https://…/p/?...`).
    /// Retourne nil si les champs requis sont manquants ou invalides.
    static func payload(from url: URL) -> Payload? {
        guard isPrayerLink(url),
              let comps = URLComponents(url: url, resolvingAgainstBaseURL: false)
        else { return nil }

        let q = Dictionary(
            (comps.queryItems ?? []).map { ($0.name, $0.value ?? "") },
            uniquingKeysWith: { first, _ in first }
        )

        guard let typeRaw = q["type"], let type = PrayerType(rawValue: typeRaw),
              let relRaw = q["rel"], let rel = RelationType(rawValue: relRaw),
              let name = q["name"]?.trimmingCharacters(in: .whitespacesAndNewlines), !name.isEmpty,
              let mother = q["mother"]?.trimmingCharacters(in: .whitespacesAndNewlines), !mother.isEmpty
        else { return nil }

        let death = q["death"].flatMap { dateFormatter.date(from: $0) }
        return Payload(
            prayerType: type,
            relativeFirstName: name,
            relationType: rel,
            motherFirstName: mother,
            civilDateOfDeath: death
        )
    }

    /// Construit un `SavedPrayerIntent` complet à partir d'un payload :
    /// régénère la séquence de lettres et la date hébraïque. Les rappels sont
    /// désactivés par défaut — le destinataire choisit lui-même de les activer.
    static func makeIntent(from p: Payload) -> SavedPrayerIntent {
        let sequence = LetterSequenceGenerator.generate(
            relativeName: p.relativeFirstName,
            relation: p.relationType,
            motherName: p.motherFirstName,
            prayerType: p.prayerType
        )
        return SavedPrayerIntent(
            title: LetterSequenceGenerator.makeTitle(
                prayerType: p.prayerType,
                relativeName: p.relativeFirstName,
                relation: p.relationType,
                motherName: p.motherFirstName
            ),
            prayerType: p.prayerType,
            relativeFirstName: p.relativeFirstName,
            relationType: p.relationType,
            motherFirstName: p.motherFirstName,
            generatedLetters: sequence,
            civilDateOfDeath: p.civilDateOfDeath,
            hebrewDateOfDeath: p.civilDateOfDeath.map { MemorialCalculator.hebrewYMD(from: $0) },
            remindersEnabled: false,
            notifySevenDaysBefore: true,
            notifySameDay: true
        )
    }
}

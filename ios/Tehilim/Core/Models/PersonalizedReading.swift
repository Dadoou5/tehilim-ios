import Foundation

// MARK: - Type de prière

/// Type de prière personnalisée — détermine si `נשמה` est ajouté en fin de séquence.
enum PrayerType: String, Codable, CaseIterable, Identifiable, Hashable {
    case malade
    case defunt

    var id: String { rawValue }

    var labelFR: String {
        switch self {
        case .malade: return "Malade"
        case .defunt: return "Défunt"
        }
    }

    /// Label du bouton "Sauvegarder en tant que…" selon le type.
    var saveActionTitle: String {
        switch self {
        case .malade: return "Refoua Cheléma"
        case .defunt: return "Lelouy Nichmat"
        }
    }
}

// MARK: - Lien de parenté

/// Lien de parenté entre la personne et sa mère, exprimé en hébreu.
enum RelationType: String, Codable, CaseIterable, Identifiable, Hashable {
    case ben   // בן — fils
    case bat   // בת — fille

    var id: String { rawValue }

    /// Expression hébraïque (« בן » ou « בת »).
    var hebrew: String {
        switch self {
        case .ben: return "בן"
        case .bat: return "בת"
        }
    }

    /// Caractères hébreux composant le lien — utilisé par le générateur de séquence.
    var hebrewCharacters: [Character] {
        Array(hebrew)
    }
}

// MARK: - Source d'une lettre dans la séquence

/// Origine d'une lettre dans la séquence de lecture personnalisée.
/// Permet d'afficher un label contextuel (« proche », « mère », etc.).
enum LetterSource: String, Codable, Hashable {
    case proche
    case lien
    case mere
    case neshama

    /// Label affiché à droite de chaque ligne de la séquence.
    ///
    /// V1.10.7 — passe par `L()` (helper qui force le lookup via le swizzle
    /// Bundle V2.1.b) pour suivre la langue de l'app. La valeur retournée
    /// est déjà localisée. `נשמה` reste en hébreu dans toutes les langues.
    var label: String {
        switch self {
        case .proche:  return L("proche")
        case .lien:    return L("lien")
        case .mere:    return L("mère")
        case .neshama: return "נשמה"
        }
    }

    /// Backward-compat : ancien nom utilisé par d'éventuels call sites
    /// avant la refacto V1.10.7. Délégue à `label`.
    @available(*, deprecated, renamed: "label")
    var labelFR: String { label }
}

// MARK: - Item de la séquence de lecture

/// Une lettre individuelle dans la séquence de lecture personnalisée.
///
/// Note : `character` est stocké en `String` (à 1 caractère) pour permettre
/// la conformance `Codable` automatique. L'accesseur `letter` retourne le
/// `Character` correspondant pour respecter l'esprit du modèle initial.
struct ReadingLetterItem: Identifiable, Codable, Hashable {
    let id: UUID
    /// Lettre hébraïque sous sa forme **de base** (les finales sont déjà mappées).
    let character: String
    let source: LetterSource
    /// Position dans la séquence (0-indexed).
    let orderIndex: Int
    /// Clé utilisée pour retrouver la section du Tehilim 119 (= la lettre de base).
    let psalmLetterKey: String

    init(
        id: UUID = UUID(),
        character: Character,
        source: LetterSource,
        orderIndex: Int,
        psalmLetterKey: String
    ) {
        self.id = id
        self.character = String(character)
        self.source = source
        self.orderIndex = orderIndex
        self.psalmLetterKey = psalmLetterKey
    }

    /// Le `Character` Hebrew correspondant.
    var letter: Character { character.first ?? " " }
}

// MARK: - Date hébraïque (Y/M/D)

/// Date hébraïque mémorisée — cache des composantes calculées par
/// `Calendar(identifier: .hebrew)` à partir d'une date civile. Le `month`
/// suit l'indexation Apple Hebrew (1=Tishri, 6=Adar I en année embolismique,
/// 6=Adar unique en année commune, 7=Adar II / Nisan selon le cas).
///
/// V1.10.7 — introduit pour la feature Commémoration.
struct HebrewYMD: Codable, Hashable {
    let year: Int
    let month: Int
    let day: Int
}

// MARK: - Prière sauvegardée

/// Objet sauvegardé (Refoua Cheléma / Lelouy Nichmat) — persisté sur disque.
///
/// V1.10.7 — ajout des 5 champs `civilDateOfDeath`, `hebrewDateOfDeath`,
/// `remindersEnabled`, `notifySevenDaysBefore`, `notifySameDay` pour la
/// feature Commémoration. Le decoder custom assure la rétrocompat : les
/// intents existants (V1.10.6 et avant) sont lus sans erreur, les nouveaux
/// champs prennent leurs valeurs par défaut.
struct SavedPrayerIntent: Identifiable, Codable, Hashable {
    let id: UUID
    /// Titre auto-généré, ex. « Refoua Cheléma — יוסף בן שרה ».
    var title: String
    var prayerType: PrayerType
    /// Prénom (en hébreu) du proche concerné.
    var relativeFirstName: String
    var relationType: RelationType
    /// Prénom (en hébreu) de la mère.
    var motherFirstName: String
    /// Séquence de lettres pré-calculée à la sauvegarde — évite de recalculer.
    var generatedLetters: [ReadingLetterItem]
    var createdAt: Date
    /// Position de la dernière lettre lue (pour « Reprendre la lecture »).
    var lastReadIndex: Int?

    // MARK: V1.10.7 — Commémoration
    /// Date civile du décès saisie par l'utilisateur. Optionnelle : un Lelouy
    /// Nichmat peut être généré sans date, le calcul azcara nécessite cette
    /// info.
    var civilDateOfDeath: Date?
    /// Cache de la date hébraïque dérivée de `civilDateOfDeath` — évite de
    /// refaire la conversion à chaque accès et stabilise le calcul en cas
    /// d'évolution future des règles.
    var hebrewDateOfDeath: HebrewYMD?
    /// L'utilisateur veut-il recevoir des rappels pour la prochaine azcara ?
    var remindersEnabled: Bool
    /// Si true ET `remindersEnabled` : notification 7 jours avant.
    var notifySevenDaysBefore: Bool
    /// Si true ET `remindersEnabled` : notification le jour même (à 9h locale).
    var notifySameDay: Bool

    init(
        id: UUID = UUID(),
        title: String,
        prayerType: PrayerType,
        relativeFirstName: String,
        relationType: RelationType,
        motherFirstName: String,
        generatedLetters: [ReadingLetterItem],
        createdAt: Date = Date(),
        lastReadIndex: Int? = nil,
        civilDateOfDeath: Date? = nil,
        hebrewDateOfDeath: HebrewYMD? = nil,
        remindersEnabled: Bool = false,
        notifySevenDaysBefore: Bool = true,
        notifySameDay: Bool = true
    ) {
        self.id = id
        self.title = title
        self.prayerType = prayerType
        self.relativeFirstName = relativeFirstName
        self.relationType = relationType
        self.motherFirstName = motherFirstName
        self.generatedLetters = generatedLetters
        self.createdAt = createdAt
        self.lastReadIndex = lastReadIndex
        self.civilDateOfDeath = civilDateOfDeath
        self.hebrewDateOfDeath = hebrewDateOfDeath
        self.remindersEnabled = remindersEnabled
        self.notifySevenDaysBefore = notifySevenDaysBefore
        self.notifySameDay = notifySameDay
    }

    /// V1.10.7 — decoder custom pour absorber sans erreur les anciens
    /// intents (V1.10.6 et avant) qui n'ont pas les champs Commémoration.
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        self.id = try c.decode(UUID.self, forKey: .id)
        self.title = try c.decode(String.self, forKey: .title)
        self.prayerType = try c.decode(PrayerType.self, forKey: .prayerType)
        self.relativeFirstName = try c.decode(String.self, forKey: .relativeFirstName)
        self.relationType = try c.decode(RelationType.self, forKey: .relationType)
        self.motherFirstName = try c.decode(String.self, forKey: .motherFirstName)
        self.generatedLetters = try c.decode([ReadingLetterItem].self, forKey: .generatedLetters)
        self.createdAt = try c.decode(Date.self, forKey: .createdAt)
        self.lastReadIndex = try c.decodeIfPresent(Int.self, forKey: .lastReadIndex)
        // Nouveaux champs V1.10.7 — `decodeIfPresent` + défauts garantissent
        // la rétrocompat avec les payloads JSON sans ces clés.
        self.civilDateOfDeath = try c.decodeIfPresent(Date.self, forKey: .civilDateOfDeath)
        self.hebrewDateOfDeath = try c.decodeIfPresent(HebrewYMD.self, forKey: .hebrewDateOfDeath)
        self.remindersEnabled = try c.decodeIfPresent(Bool.self, forKey: .remindersEnabled) ?? false
        self.notifySevenDaysBefore = try c.decodeIfPresent(Bool.self, forKey: .notifySevenDaysBefore) ?? true
        self.notifySameDay = try c.decodeIfPresent(Bool.self, forKey: .notifySameDay) ?? true
    }

    /// Description hébraïque compacte, ex. « יוסף בן שרה » ou « שרה בת מרים ».
    var hebrewSubject: String {
        "\(relativeFirstName) \(relationType.hebrew) \(motherFirstName)"
    }
}

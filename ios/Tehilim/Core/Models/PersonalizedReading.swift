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

// MARK: - Prière sauvegardée

/// Objet sauvegardé (Refoua Cheléma / Lelouy Nichmat) — persisté sur disque.
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

    init(
        id: UUID = UUID(),
        title: String,
        prayerType: PrayerType,
        relativeFirstName: String,
        relationType: RelationType,
        motherFirstName: String,
        generatedLetters: [ReadingLetterItem],
        createdAt: Date = Date(),
        lastReadIndex: Int? = nil
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
    }

    /// Description hébraïque compacte, ex. « יוסף בן שרה » ou « שרה בת מרים ».
    var hebrewSubject: String {
        "\(relativeFirstName) \(relationType.hebrew) \(motherFirstName)"
    }
}

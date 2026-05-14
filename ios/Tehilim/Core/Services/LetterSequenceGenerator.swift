import Foundation

/// Génère la séquence de lecture personnalisée pour le Tehilim 119.
///
/// **Règle métier non-négociable** : `נשמה` n'est ajouté qu'en cas de **Défunt**.
/// Jamais l'utilisateur ne tape « נשמה » lui-même — c'est l'app qui l'ajoute.
///
/// **Ordre de concaténation** :
/// 1. Lettres du prénom du proche
/// 2. Lettres de `בן` ou `בת`
/// 3. Lettres du prénom de la mère
/// 4. Lettres de `נשמה` (uniquement si Défunt)
enum LetterSequenceGenerator {

    /// Génère la séquence finale.
    /// - Parameters:
    ///   - relativeName: prénom du proche (Hebrew, validé en amont)
    ///   - relation: bן ou bת
    ///   - motherName: prénom de la mère (Hebrew, validé en amont)
    ///   - prayerType: malade ou defunt — pilote l'ajout de נשמה
    /// - Returns: séquence ordonnée d'items prêts à afficher / naviguer.
    static func generate(
        relativeName: String,
        relation: RelationType,
        motherName: String,
        prayerType: PrayerType
    ) -> [ReadingLetterItem] {
        var items: [ReadingLetterItem] = []
        var index = 0

        // 1. Prénom du proche
        for letter in HebrewLetterMapper.baseLetters(in: relativeName) {
            items.append(
                ReadingLetterItem(
                    character: letter,
                    source: .proche,
                    orderIndex: index,
                    psalmLetterKey: String(letter)
                )
            )
            index += 1
        }

        // 2. Lien (בן / בת)
        for letter in relation.hebrewCharacters {
            let base = HebrewLetterMapper.toBase(letter)
            items.append(
                ReadingLetterItem(
                    character: base,
                    source: .lien,
                    orderIndex: index,
                    psalmLetterKey: String(base)
                )
            )
            index += 1
        }

        // 3. Prénom de la mère
        for letter in HebrewLetterMapper.baseLetters(in: motherName) {
            items.append(
                ReadingLetterItem(
                    character: letter,
                    source: .mere,
                    orderIndex: index,
                    psalmLetterKey: String(letter)
                )
            )
            index += 1
        }

        // 4. נשמה (uniquement Défunt)
        if prayerType == .defunt {
            for letter in neshamaLetters {
                items.append(
                    ReadingLetterItem(
                        character: letter,
                        source: .neshama,
                        orderIndex: index,
                        psalmLetterKey: String(letter)
                    )
                )
                index += 1
            }
        }

        return items
    }

    /// Génère un titre lisible pour la sauvegarde.
    /// Ex. « Refoua Cheléma — יוסף בן שרה » ou « Lelouy Nichmat — דוד בן רחל ».
    static func makeTitle(
        prayerType: PrayerType,
        relativeName: String,
        relation: RelationType,
        motherName: String
    ) -> String {
        "\(prayerType.saveActionTitle) — \(relativeName) \(relation.hebrew) \(motherName)"
    }

    // MARK: - Constantes

    /// Lettres composant « נשמה » (âme) — ajoutées en fin de séquence pour Défunt.
    private static let neshamaLetters: [Character] = ["נ", "ש", "מ", "ה"]
}

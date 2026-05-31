import SwiftUI

/// Helpers et constantes pour adapter le layout entre iPhone (compact) et iPad (regular).
///
/// Philosophie : on ne crée pas deux UIs séparées. On garde la même hiérarchie de vues
/// SwiftUI et on ajuste les paramètres numériques (colonnes, max width, padding) selon
/// la `horizontalSizeClass` de l'environnement.
enum AdaptiveLayout {

    /// Largeur maximale d'une colonne de texte lisible.
    /// 700pt ≈ 70 caractères/ligne en typo standard — la « longueur idéale » selon
    /// la typographie classique (Bringhurst). Au-delà, l'œil saute de lignes.
    static let readingMaxWidth: CGFloat = 700

    /// Padding latéral à appliquer autour des cartes / listes.
    /// - iPhone : 16pt (déjà la norme du projet)
    /// - iPad   : 24pt (plus aéré, profite de l'espace)
    static func horizontalPadding(for sizeClass: UserInterfaceSizeClass?) -> CGFloat {
        sizeClass == .regular ? 24 : 16
    }

    /// Nombre de colonnes pour la grille « Explorer » de l'accueil.
    /// 6 cartes au total → 2 colonnes sur iPhone, 3 sur iPad (2 lignes équilibrées).
    static func exploreColumnCount(for sizeClass: UserInterfaceSizeClass?) -> Int {
        sizeClass == .regular ? 3 : 2
    }

    /// Nombre de colonnes pour la grille des 22 lettres hébraïques (Tehilim 119).
    /// 22 lettres → 4 colonnes (6 lignes) sur iPhone, 6 colonnes (4 lignes) sur iPad.
    /// Choix de 6 plutôt que 8 : pavés plus grands, plus aérés, meilleur pour l'œil
    /// et pour la nouvelle info enrichie (nom de lettre + range de versets).
    static func psalm119ColumnCount(for sizeClass: UserInterfaceSizeClass?) -> Int {
        sizeClass == .regular ? 6 : 4
    }

    /// Nombre de colonnes pour la grille des « Cas de la vie ».
    /// 18 cas répartis en 5 sections → 2 colonnes (iPhone) / 3 colonnes (iPad).
    /// On évite la 4ᵉ colonne pour garder des cartes assez larges pour le texte.
    static func lifeCaseColumnCount(for sizeClass: UserInterfaceSizeClass?) -> Int {
        sizeClass == .regular ? 3 : 2
    }

    /// Largeur maximale d'un **tableau de bord de cartes** (accueil, Cas de la
    /// vie). Bien plus large que `readingMaxWidth` (700, dédié au texte) : ces
    /// écrans sont des grilles de cartes, pas des colonnes de lecture — les
    /// brider à 700pt laisse d'énormes marges vides sur iPad, surtout en
    /// paysage. 1100pt exploite la largeur tout en gardant une marge douce sur
    /// 12,9" (1366pt en paysage).
    static let dashboardMaxWidth: CGFloat = 1100

    /// Colonnes **adaptatives** : SwiftUI ajuste seul le nombre de colonnes à
    /// la largeur réellement disponible → davantage de colonnes en paysage,
    /// moins en portrait, sans coder l'orientation. Le minimum par item dépend
    /// de la classe de taille (items compacts sur iPhone, plus larges sur iPad).
    /// - Parameter compactMin : largeur mini d'un item sur iPhone (compact).
    /// - Parameter regularMin : largeur mini d'un item sur iPad (regular).
    static func adaptiveColumns(
        for sizeClass: UserInterfaceSizeClass?,
        compactMin: CGFloat,
        regularMin: CGFloat,
        spacing: CGFloat = 12
    ) -> [GridItem] {
        let minWidth = sizeClass == .regular ? regularMin : compactMin
        return [GridItem(.adaptive(minimum: minWidth), spacing: spacing)]
    }

    /// Largeur minimale d'un container pour activer le mode lecture parallèle
    /// (hébreu et traduction côte-à-côte).
    /// 900pt → confortable pour 2 colonnes de ~430pt chacune.
    static let sideBySideMinWidth: CGFloat = 900

    /// En mode lecture parallèle on dépasse la limite habituelle de 700pt
    /// pour pouvoir afficher 2 colonnes de texte côte-à-côte.
    static let sideBySideMaxWidth: CGFloat = 1200

    /// Détermine si le mode parallèle doit s'activer pour un container donné.
    static func shouldUseSideBySide(
        containerWidth: CGFloat,
        sizeClass: UserInterfaceSizeClass?
    ) -> Bool {
        sizeClass == .regular && containerWidth >= sideBySideMinWidth
    }
}

/// View modifier : limite la largeur d'une vue de lecture et la centre.
/// Aucun effet sur iPhone (la vue prend déjà toute la largeur).
/// Sur iPad, cap à `maxWidth` pour garder des lignes lisibles.
struct ReadingWidthModifier: ViewModifier {
    let maxWidth: CGFloat

    func body(content: Content) -> some View {
        content
            .frame(maxWidth: maxWidth)
            .frame(maxWidth: .infinity)
    }
}

extension View {
    /// Limite la largeur du contenu de lecture (700pt max par défaut, centré).
    /// À appliquer sur le `VStack` racine à l'intérieur d'un `ScrollView`.
    /// - Parameter maxWidth: Largeur maximale du contenu. Par défaut 700pt
    ///   (lecture standard). Passer `AdaptiveLayout.sideBySideMaxWidth` (1200pt)
    ///   pour le mode lecture parallèle iPad paysage.
    func readingWidth(maxWidth: CGFloat = AdaptiveLayout.readingMaxWidth) -> some View {
        modifier(ReadingWidthModifier(maxWidth: maxWidth))
    }
}

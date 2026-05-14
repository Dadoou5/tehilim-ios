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
    /// 22 lettres → 4 colonnes (6 lignes) sur iPhone, 8 colonnes (3 lignes) sur iPad.
    static func psalm119ColumnCount(for sizeClass: UserInterfaceSizeClass?) -> Int {
        sizeClass == .regular ? 8 : 4
    }

    /// Nombre de colonnes pour la grille des « Cas de la vie ».
    /// 18 cas répartis en 5 sections → 2 colonnes (iPhone) / 3 colonnes (iPad).
    /// On évite la 4ᵉ colonne pour garder des cartes assez larges pour le texte.
    static func lifeCaseColumnCount(for sizeClass: UserInterfaceSizeClass?) -> Int {
        sizeClass == .regular ? 3 : 2
    }
}

/// View modifier : limite la largeur d'une vue de lecture et la centre.
/// Aucun effet sur iPhone (la vue prend déjà toute la largeur).
/// Sur iPad, cap à `AdaptiveLayout.readingMaxWidth` pour garder des lignes lisibles.
struct ReadingWidthModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .frame(maxWidth: AdaptiveLayout.readingMaxWidth)
            .frame(maxWidth: .infinity)
    }
}

extension View {
    /// Limite la largeur du contenu de lecture (700pt max, centré).
    /// À appliquer sur le `VStack` racine à l'intérieur d'un `ScrollView`.
    func readingWidth() -> some View {
        modifier(ReadingWidthModifier())
    }
}

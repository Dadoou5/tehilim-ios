import SwiftUI
import UniformTypeIdentifiers

/// Représentation `Transferable` d'un verset partageable en image stylisée.
///
/// **Pourquoi ce type existe** :
/// Avant V1.10.4, `VerseRowView.contextMenu` construisait deux `Image` SwiftUI
/// immédiatement (une pour `ShareLink.item:`, une pour `SharePreview.image:`)
/// via `VerseShareImageRenderer.render(...)`. Chaque rendu produit une UIImage
/// 1080×1080 @2x ≈ 18 MB. Pour un Tehilim long (Tehilim 119 = 176 versets),
/// la LazyVStack matérialise plusieurs dizaines de lignes, chaque ligne construit
/// son `contextMenu` au build (eagerly), ce qui matérialise des centaines de
/// méga-octets de pixels en RAM → OOM.
///
/// Avec ce `Transferable`, l'image n'est rendue qu'au moment où l'utilisateur
/// **choisit effectivement de partager** (iOS invoque `transferRepresentation`).
struct VerseShareTransferable: Transferable {
    let psalm: Psalm
    let verse: Verse
    let translationLang: TranslationLanguage

    static var transferRepresentation: some TransferRepresentation {
        DataRepresentation(exportedContentType: .png) { shareable in
            let image: UIImage? = await MainActor.run {
                VerseShareImageRenderer.render(
                    psalm: shareable.psalm,
                    verse: shareable.verse,
                    translationLang: shareable.translationLang
                )
            }
            guard let data = image?.pngData() else {
                throw VerseShareError.renderFailed
            }
            return data
        }
        .suggestedFileName { shareable in
            "Tehilim_\(shareable.psalm.id)_v\(shareable.verse.number).png"
        }
    }
}

enum VerseShareError: Error {
    case renderFailed
}

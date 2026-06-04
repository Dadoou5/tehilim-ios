import SwiftUI
import UIKit
import CoreImage.CIFilterBuiltins

/// Génère une image QR code à partir d'une chaîne (lien d'invitation).
/// Style « marque » : modules bleu nuit (couleur de l'icône) sur fond blanc, et
/// correction d'erreur maximale (H) pour tolérer le logo incrusté au centre.
enum QRCodeGenerator {
    private static let context = CIContext()
    private static let filter = CIFilter.qrCodeGenerator()

    /// Bleu nuit repris du fond de l'icône de lancement.
    static let moduleColor = UIColor(red: 0.055, green: 0.078, blue: 0.188, alpha: 1) // #0E1430
    /// Contour discret de la pastille du logo (même bleu nuit, atténué).
    static let brandStroke = Color(red: 0.055, green: 0.078, blue: 0.188).opacity(0.15)

    static func image(from string: String) -> UIImage? {
        filter.message = Data(string.utf8)
        filter.correctionLevel = "H"   // 30 % de redondance → supporte le logo central
        guard let qr = filter.outputImage else { return nil }

        // Recolore : modules → bleu nuit, fond → blanc (CIFalseColor).
        let false0 = CIColor(color: moduleColor)
        let false1 = CIColor(color: .white)
        let colored = qr.applyingFilter("CIFalseColor", parameters: [
            "inputColor0": false0,   // modules « allumés »
            "inputColor1": false1    // fond
        ])
        let scaled = colored.transformed(by: CGAffineTransform(scaleX: 12, y: 12))
        guard let cg = context.createCGImage(scaled, from: scaled.extent) else { return nil }
        return UIImage(cgImage: cg)
    }
}

/// Feuille d'invitation à une chaîne : QR code (présentiel) + lien (copier /
/// partager WhatsApp). C'est le point d'entrée « faire grandir la chaîne ».
struct ChainInviteSheet: View {
    let chain: TehilimChain
    @Environment(\.dismiss) private var dismiss
    @State private var copied = false

    private var link: String {
        ChainShareLink.url(forChainId: chain.id)?.absoluteString ?? ""
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    Text("Partage le QR code ou le lien — chacun rejoint la chaîne et choisit les Tehilim qu'il lira.")
                        .font(.callout).foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)

                    if let img = QRCodeGenerator.image(from: link) {
                        ZStack {
                            Image(uiImage: img)
                                .interpolation(.none)
                                .resizable().scaledToFit()
                            // Logo de marque incrusté au centre, sur une pastille
                            // blanche (le niveau de correction H couvre la zone masquée).
                            Image("ChainBrandLogo")
                                .resizable().scaledToFit()
                                .frame(width: 54, height: 54)
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                                .padding(6)
                                .background(Color.white, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                                        .stroke(QRCodeGenerator.brandStroke, lineWidth: 1)
                                )
                        }
                        .frame(width: 230, height: 230)
                        .padding(14)
                        .background(Color.white, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                .stroke(Color.dividerToken.opacity(0.4), lineWidth: 0.5)
                        )
                        .accessibilityLabel("QR code d'invitation")
                    }

                    Text(link)
                        .font(.caption.monospaced()).foregroundStyle(.secondary)
                        .multilineTextAlignment(.center).textSelection(.enabled)
                        .padding(.horizontal)

                    ShareLink(item: ChainShareLink.shareMessage(for: chain)) {
                        Label("Partager le lien", systemImage: "square.and.arrow.up")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent).tint(.accentMain).controlSize(.large)

                    Button {
                        UIPasteboard.general.string = link
                        copied = true
                    } label: {
                        Label(copied ? "Lien copié" : "Copier le lien",
                              systemImage: copied ? "checkmark" : "doc.on.doc")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered).controlSize(.large)
                }
                .padding()
                .readingWidth(maxWidth: 520)
            }
            .background(Color.bgPrimary)
            .navigationTitle("Inviter")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("OK") { dismiss() }
                }
            }
        }
    }
}

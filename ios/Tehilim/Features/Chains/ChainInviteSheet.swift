import SwiftUI
import UIKit
import CoreImage.CIFilterBuiltins

/// Génère une image QR code à partir d'une chaîne (lien d'invitation).
enum QRCodeGenerator {
    private static let context = CIContext()
    private static let filter = CIFilter.qrCodeGenerator()

    static func image(from string: String) -> UIImage? {
        filter.message = Data(string.utf8)
        filter.correctionLevel = "M"
        guard let output = filter.outputImage?
                .transformed(by: CGAffineTransform(scaleX: 12, y: 12)),
              let cg = context.createCGImage(output, from: output.extent)
        else { return nil }
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
                        Image(uiImage: img)
                            .interpolation(.none)
                            .resizable().scaledToFit()
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

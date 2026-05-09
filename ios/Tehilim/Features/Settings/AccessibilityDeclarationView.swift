import SwiftUI

struct AccessibilityDeclarationView: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {

                section("Engagement") {
                    Text("L'application Tehilim s'engage à rendre son contenu accessible conformément au Référentiel Général d'Amélioration de l'Accessibilité (RGAA 4.1.2) et au standard WCAG 2.1 niveau AA, applicables aux applications mobiles.")
                }

                section("État de conformité") {
                    Text("Statut : conformité partielle (auto-déclaration).").bold()
                    Text("Cette déclaration est établie sur la base d'un auto-audit. Un audit externe est prévu avant la mise en production.")
                        .foregroundStyle(.secondary)
                }

                section("Caractéristiques d'accessibilité prises en charge") {
                    bullet("Compatibilité complète avec VoiceOver.")
                    bullet("Respect de la taille de texte système (Dynamic Type) jusqu'à AX5.")
                    bullet("Modes clair, sombre, et automatique.")
                    bullet("Contrastes de texte conformes au niveau AA (≥ 4.5:1).")
                    bullet("Cibles tactiles supérieures ou égales à 44 × 44 points.")
                    bullet("Aucune information transmise uniquement par la couleur.")
                    bullet("Respect du paramètre « Réduire les animations ».")
                    bullet("Lecture VoiceOver de l'hébreu en sens RTL et de la traduction en LTR, dans cet ordre.")
                }

                section("Limites connues") {
                    bullet("Le texte hébreu est lu phonétiquement par VoiceOver selon les capacités de la voix sélectionnée par l'utilisateur·rice.")
                    bullet("La grille des 22 lettres du Tehilim 119 peut nécessiter un défilement avec une très grande taille de texte.")
                }

                section("Voies de recours") {
                    Text("Pour signaler un problème d'accessibilité : contact à définir avant publication App Store.")
                        .foregroundStyle(.secondary)
                }

                section("Référentiel et version") {
                    Text("RGAA 4.1.2 — version de l'application : \(version)")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .padding()
        }
        .navigationTitle("Accessibilité")
    }

    @ViewBuilder
    private func section<C: View>(_ title: String, @ViewBuilder content: () -> C) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).font(.headline).accessibilityAddTraits(.isHeader)
            content()
        }
    }

    private func bullet(_ text: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Text("•").foregroundStyle(.secondary)
            Text(text)
        }
    }

    private var version: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
    }
}

import SwiftUI
import UIKit

/// `TextField` qui demande à iOS de basculer **automatiquement sur le clavier hébreu**
/// quand l'utilisateur entre dans le champ, à condition que le clavier hébreu soit
/// activé dans Réglages → Général → Clavier → Claviers.
///
/// Mécanisme : on override `textInputMode` sur une sous-classe d'`UITextField`,
/// en cherchant un input mode dont `primaryLanguage` commence par « he ».
/// Si aucun clavier hébreu n'est installé, iOS retombe sur le clavier par défaut
/// sans erreur.
///
/// Le filtre live (`HebrewLetterMapper.filterHebrew`) reste actif comme garde-fou :
/// même si l'utilisateur garde son clavier français, seuls les caractères hébreux
/// sont conservés.
struct HebrewKeyboardTextField: UIViewRepresentable {
    @Binding var text: String
    var placeholder: String

    func makeUIView(context: Context) -> UITextField {
        let tf = HebrewPreferredTextField()
        tf.placeholder = placeholder
        tf.textAlignment = .right
        tf.semanticContentAttribute = .forceRightToLeft
        tf.autocorrectionType = .no
        tf.autocapitalizationType = .none
        tf.spellCheckingType = .no
        tf.smartQuotesType = .no
        tf.smartDashesType = .no
        tf.smartInsertDeleteType = .no
        tf.font = UIFont.preferredFont(forTextStyle: .body)
        tf.adjustsFontForContentSizeCategory = true
        tf.delegate = context.coordinator
        tf.addTarget(
            context.coordinator,
            action: #selector(Coordinator.editingChanged(_:)),
            for: .editingChanged
        )
        return tf
    }

    func updateUIView(_ uiView: UITextField, context: Context) {
        if uiView.text != text {
            uiView.text = text
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator(parent: self) }

    final class Coordinator: NSObject, UITextFieldDelegate {
        let parent: HebrewKeyboardTextField
        init(parent: HebrewKeyboardTextField) { self.parent = parent }

        @objc func editingChanged(_ tf: UITextField) {
            let raw = tf.text ?? ""
            let filtered = HebrewLetterMapper.filterHebrew(raw)
            if raw != filtered {
                tf.text = filtered
            }
            parent.text = filtered
        }
    }
}

/// Sous-classe d'`UITextField` qui force iOS à proposer le clavier hébreu.
private final class HebrewPreferredTextField: UITextField {
    /// Override : iOS consulte cette propriété quand le champ devient first responder.
    /// On lui retourne le 1ᵉʳ input mode dont la langue primaire commence par « he »
    /// (he, he-IL…). Si rien n'est trouvé, on retourne `nil` → iOS choisit lui-même.
    override var textInputMode: UITextInputMode? {
        UITextInputMode.activeInputModes.first { mode in
            mode.primaryLanguage?.hasPrefix("he") == true
        } ?? super.textInputMode
    }
}

// MARK: - Utility

extension UITextInputMode {
    /// `true` si l'utilisateur a activé un clavier hébreu dans ses Réglages.
    /// Utilisé pour afficher un encart d'aide en cas contraire.
    static var isHebrewKeyboardInstalled: Bool {
        activeInputModes.contains { $0.primaryLanguage?.hasPrefix("he") == true }
    }
}

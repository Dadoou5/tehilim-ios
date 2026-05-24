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
///
/// V1.10.7 — paramètres `returnKeyType` + `isFocused` (Binding) + `onReturn`
/// pour permettre au parent de chaîner deux champs : « Suivant » sur le 1er
/// transfère le focus au 2nd sans refermer le clavier ; « Terminé » sur le
/// 2nd ferme le clavier. Parité avec Android `HebrewFieldImeAction` Next/Done.
struct HebrewKeyboardTextField: UIViewRepresentable {
    @Binding var text: String
    var placeholder: String
    var returnKeyType: UIReturnKeyType = .default
    /// Binding optionnel : si fourni, le parent peut prendre/perdre le focus
    /// programmatiquement (set à `true` → becomeFirstResponder, set à `false`
    /// → resignFirstResponder). Si `nil`, le champ se comporte de manière
    /// classique (focus piloté uniquement par l'utilisateur).
    var isFocused: Binding<Bool>? = nil
    /// Appelé quand l'utilisateur tape la touche return du clavier (Next ou
    /// Done). Le parent décide quoi faire (focus suivant, fermer clavier…).
    var onReturn: () -> Void = {}

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
        tf.returnKeyType = returnKeyType
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
        // V1.10.7 — pattern standard UIViewRepresentable : on rafraîchit la
        // référence parent du Coordinator pour qu'il pointe vers la version
        // courante du struct (closures `onReturn` + bindings à jour). Sans
        // ça, les callbacks delegate utiliseraient une copie stale des
        // closures, source de bugs subtils de focus chain.
        context.coordinator.parent = self

        if uiView.text != text {
            uiView.text = text
        }
        // Applique l'éventuelle demande de focus venue du parent. Garde-fou :
        // on n'agit que si l'état actuel diffère, sinon on tombe dans une
        // boucle de mises à jour SwiftUI lors d'un re-render.
        if let isFocused = isFocused?.wrappedValue {
            if isFocused && !uiView.isFirstResponder {
                uiView.becomeFirstResponder()
            } else if !isFocused && uiView.isFirstResponder {
                uiView.resignFirstResponder()
            }
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator(parent: self) }

    final class Coordinator: NSObject, UITextFieldDelegate {
        // `var` (et pas `let`) : updateUIView le rafraîchit à chaque
        // re-render pour récupérer les nouvelles closures du parent.
        var parent: HebrewKeyboardTextField
        init(parent: HebrewKeyboardTextField) { self.parent = parent }

        @objc func editingChanged(_ tf: UITextField) {
            let raw = tf.text ?? ""
            let filtered = HebrewLetterMapper.filterHebrew(raw)
            if raw != filtered {
                tf.text = filtered
            }
            parent.text = filtered
        }

        // V1.10.7 — sync de la focus state vers le @State du parent quand
        // le focus change suite à un tap utilisateur direct sur le champ.
        // Garde le binding et l'état UIKit cohérents dans les deux sens.
        func textFieldDidBeginEditing(_ textField: UITextField) {
            if let binding = parent.isFocused, !binding.wrappedValue {
                DispatchQueue.main.async { binding.wrappedValue = true }
            }
        }

        func textFieldDidEndEditing(_ textField: UITextField) {
            if let binding = parent.isFocused, binding.wrappedValue {
                DispatchQueue.main.async { binding.wrappedValue = false }
            }
        }

        // V1.10.7 — touche return : on délègue au parent (qui décide de
        // passer au champ suivant ou de fermer le clavier). On renvoie
        // `false` pour ne pas insérer de saut de ligne dans le champ
        // (singleLine de toute façon).
        func textFieldShouldReturn(_ textField: UITextField) -> Bool {
            parent.onReturn()
            return false
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

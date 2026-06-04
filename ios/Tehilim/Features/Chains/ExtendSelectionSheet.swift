import SwiftUI

/// Feuille de prolongation de la sélection d'une chaîne (maître).
/// Le créateur choisit une **durée** à ajouter (depuis l'échéance courante, ou
/// depuis maintenant si elle est déjà dépassée). À la confirmation, l'app appelle
/// `extend_chain_selection` (réarme les rappels + re-notifie les participants).
struct ExtendSelectionSheet: View {
    @Binding var hours: Int
    /// Échéance actuelle, pour afficher la nouvelle date résultante.
    let currentDeadline: Date
    let onConfirm: () -> Void
    @Environment(\.dismiss) private var dismiss

    // Prolongation en heures uniquement, plafonnée à 48 h.
    private let durationChoices: [Int] = [1, 3, 6, 12, 24, 36, 48]

    /// Base de calcul : on prolonge depuis l'échéance si elle est dans le futur,
    /// sinon depuis maintenant (chaîne déjà expirée).
    private var newDeadline: Date {
        max(currentDeadline, Date()).addingTimeInterval(TimeInterval(hours) * 3600)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker("Prolonger de", selection: $hours) {
                        ForEach(durationChoices, id: \.self) { h in
                            Text("\(h) h").tag(h)
                        }
                    }
                } footer: {
                    Text("Nouvelle fin de sélection : \(newDeadline.formatted(date: .abbreviated, time: .shortened)). Les participants seront prévenus et les rappels repartiront.")
                }
            }
            .navigationTitle("Prolonger la sélection")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Prolonger") { onConfirm(); dismiss() }
                }
            }
        }
    }
}

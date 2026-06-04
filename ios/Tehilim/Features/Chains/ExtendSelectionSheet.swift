import SwiftUI

/// Feuille de prolongation de la sélection d'une chaîne (maître).
/// Le créateur choisit une nouvelle échéance ; à la confirmation, l'app appelle
/// `extend_chain_selection` (réarme les rappels + re-notifie les participants).
struct ExtendSelectionSheet: View {
    @Binding var date: Date
    let onConfirm: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    DatePicker("Nouvelle échéance",
                               selection: $date,
                               in: Date().addingTimeInterval(3_600)...,
                               displayedComponents: [.date, .hourAndMinute])
                } footer: {
                    Text("Les participants seront prévenus que la sélection est prolongée, et les rappels repartiront.")
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

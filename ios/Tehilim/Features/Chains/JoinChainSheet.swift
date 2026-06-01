import SwiftUI

/// Saisie du nom pour rejoindre une chaîne. Le nom est **visible de tous** les
/// participants. Clavier système (émojis OK).
struct JoinChainSheet: View {
    @Environment(\.dismiss) private var dismiss
    let onJoin: (String) -> Void

    @State private var name: String = ""

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Ton nom", text: $name)
                } footer: {
                    Text("Ton nom sera visible par tous les participants de la chaîne.")
                }
            }
            .navigationTitle("Rejoindre la chaîne")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Rejoindre") {
                        onJoin(name.trimmingCharacters(in: .whitespaces))
                        dismiss()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
    }
}

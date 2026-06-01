import SwiftUI

/// Formulaire de création d'une chaîne de Tehilim. Le clavier est le clavier
/// **système** par défaut (émojis disponibles) — simple `TextField`.
struct CreateChainView: View {
    @EnvironmentObject private var container: AppContainer
    @Environment(\.dismiss) private var dismiss

    /// Appelé avec l'id de la chaîne créée → le parent ouvre le détail.
    let onCreated: (String) -> Void

    @State private var name: String = ""
    @State private var intention: ChainIntention = .lelouy
    @State private var detail: String = ""
    @State private var selectionHours: Int = 24
    @State private var readingDeadline: Date = Calendar.current.date(byAdding: .day, value: 7, to: Date()) ?? Date()
    @State private var creatorName: String = ""

    @State private var isCreating = false
    @State private var errorMessage: String?

    private let durationChoices: [Int] = [1, 3, 6, 12, 24, 48, 72]

    private var canCreate: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty
            && !creatorName.trimmingCharacters(in: .whitespaces).isEmpty
            && readingDeadline > Date().addingTimeInterval(TimeInterval(selectionHours) * 3600)
            && !isCreating
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Nom de la chaîne", text: $name)
                    Picker("Intention", selection: $intention) {
                        ForEach(ChainIntention.allCases) { kind in
                            Text(LocalizedStringKey(kind.titleKey)).tag(kind)
                        }
                    }
                    TextField(LocalizedStringKey(detailPlaceholder), text: $detail)
                } header: {
                    Text("Intention")
                } footer: {
                    Text("Tu peux ajouter des émojis. Le sujet apparaîtra : « \(intention.titleKeyLocalized) — \(detail.isEmpty ? "…" : detail) ».")
                }

                Section {
                    Picker("Durée de sélection", selection: $selectionHours) {
                        ForEach(durationChoices, id: \.self) { h in
                            Text(h < 24 ? "\(h) h" : "\(h / 24) j").tag(h)
                        }
                    }
                    DatePicker("Fin de lecture", selection: $readingDeadline,
                               in: Date()..., displayedComponents: [.date, .hourAndMinute])
                } header: {
                    Text("Délais")
                } footer: {
                    Text("Pendant la sélection, chacun choisit ses Tehilim. Ensuite la chaîne passe en lecture seule jusqu'à la fin de lecture.")
                }

                Section {
                    TextField("Ton nom (visible de tous)", text: $creatorName)
                } header: {
                    Text("Toi")
                } footer: {
                    Text("Ton nom et l'intention sont enregistrés dans le cloud le temps de la chaîne, puis supprimés automatiquement après la lecture.")
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage).foregroundStyle(.red).font(.callout)
                    }
                }
            }
            .navigationTitle("Nouvelle chaîne")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Créer") { Task { await create() } }
                        .disabled(!canCreate)
                }
            }
            .overlay {
                if isCreating { ProgressView().controlSize(.large) }
            }
        }
    }

    private var detailPlaceholder: String {
        switch intention {
        case .lelouy:   return "Prénom du défunt"
        case .refoua:   return "Prénom du malade"
        case .reussite: return "Am Israël, un proche…"
        }
    }

    private func create() async {
        isCreating = true
        errorMessage = nil
        do {
            let id = try await container.chains.createChain(
                name: name.trimmingCharacters(in: .whitespaces),
                intention: intention,
                detail: detail.trimmingCharacters(in: .whitespaces),
                selectionDuration: TimeInterval(selectionHours) * 3600,
                readingDeadline: readingDeadline,
                creatorName: creatorName.trimmingCharacters(in: .whitespaces)
            )
            container.chainArchive.remember(id)
            isCreating = false
            dismiss()
            onCreated(id)
        } catch {
            isCreating = false
            errorMessage = "Création impossible. Vérifie ta connexion."
        }
    }
}

extension ChainIntention {
    /// Libellé localisé (résout la clé via le bundle swizzlé).
    var titleKeyLocalized: String { NSLocalizedString(titleKey, comment: "") }
}

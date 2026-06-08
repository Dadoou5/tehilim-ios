import SwiftUI

/// Formulaire de **création** ou d'**édition** d'une chaîne de Tehilim. Clavier
/// système (émojis dispo). En édition, le créateur ajuste nom / intention /
/// détail et les délais (avant distribution).
struct CreateChainView: View {
    @EnvironmentObject private var container: AppContainer
    @Environment(\.dismiss) private var dismiss

    /// Appelé avec l'id de la chaîne créée → le parent ouvre le détail.
    let onCreated: (String) -> Void
    /// Non nil = mode édition.
    let editing: TehilimChain?

    @State private var name: String
    @State private var intention: ChainIntention
    @State private var detail: String
    @State private var selectionHours: Int
    @State private var readingDeadline: Date
    @State private var selectionDeadline: Date
    @State private var creatorName: String

    @State private var isCreating = false
    @State private var errorMessage: String?

    private let durationChoices: [Int] = [1, 3, 6, 12, 24, 48, 72]
    private var isEditing: Bool { editing != nil }

    init(editing: TehilimChain? = nil, onCreated: @escaping (String) -> Void) {
        self.editing = editing
        self.onCreated = onCreated
        _name = State(initialValue: editing?.name ?? "")
        _intention = State(initialValue: editing?.intentionType ?? .lelouy)
        _detail = State(initialValue: editing?.intentionDetail ?? "")
        _selectionHours = State(initialValue: 24)
        _readingDeadline = State(initialValue: editing?.readingDeadline
            ?? Calendar.current.date(byAdding: .day, value: 7, to: Date()) ?? Date())
        _selectionDeadline = State(initialValue: editing?.selectionDeadline
            ?? Date().addingTimeInterval(24 * 3600))
        _creatorName = State(initialValue: editing?.creatorName ?? "")
    }

    private var canCreate: Bool {
        let nameOK = !name.trimmingCharacters(in: .whitespaces).isEmpty
        if isEditing {
            return nameOK && readingDeadline > selectionDeadline && !isCreating
        }
        return nameOK
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
                    if isEditing {
                        DatePicker("Fin de la sélection", selection: $selectionDeadline,
                                   displayedComponents: [.date, .hourAndMinute])
                    } else {
                        Picker("Durée de sélection", selection: $selectionHours) {
                            ForEach(durationChoices, id: \.self) { h in
                                Text(h < 24 ? "\(h) h" : "\(h / 24) j").tag(h)
                            }
                        }
                    }
                    DatePicker("Fin de lecture", selection: $readingDeadline,
                               in: Date()..., displayedComponents: [.date, .hourAndMinute])
                } header: {
                    Text("Délais")
                } footer: {
                    Text("Pendant la sélection, chacun choisit ses Tehilim. Ensuite la chaîne passe en lecture seule jusqu'à la fin de lecture.")
                }

                if !isEditing {
                    Section {
                        TextField("Ton nom (visible de tous)", text: $creatorName)
                    } header: {
                        Text("Toi")
                    } footer: {
                        Text("Jusqu'à 50 participants par chaîne. Ton nom et l'intention sont enregistrés dans le cloud le temps de la chaîne, puis supprimés automatiquement après la lecture.")
                    }
                }

                if let errorMessage {
                    Section {
                        Text(LocalizedStringKey(errorMessage)).foregroundStyle(.red).font(.callout)
                    }
                }
            }
            .navigationTitle(isEditing ? "Modifier la chaîne" : "Nouvelle chaîne")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isEditing ? "Enregistrer" : "Créer") { Task { await save() } }
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

    private func save() async {
        isCreating = true
        errorMessage = nil
        do {
            if let editing {
                try await container.chains.updateChain(
                    chainId: editing.id,
                    name: name.trimmingCharacters(in: .whitespaces),
                    intention: intention,
                    detail: detail.trimmingCharacters(in: .whitespaces),
                    selectionDeadline: selectionDeadline,
                    readingDeadline: readingDeadline)
                isCreating = false
                dismiss()
            } else {
                let id = try await container.chains.createChain(
                    name: name.trimmingCharacters(in: .whitespaces),
                    intention: intention,
                    detail: detail.trimmingCharacters(in: .whitespaces),
                    selectionDuration: TimeInterval(selectionHours) * 3600,
                    readingDeadline: readingDeadline,
                    creatorName: creatorName.trimmingCharacters(in: .whitespaces))
                container.chainArchive.remember(id)
                isCreating = false
                dismiss()
                onCreated(id)
            }
        } catch {
            isCreating = false
            if "\(error)".contains("TOO_MANY_OPEN_CHAINS") {
                errorMessage = "Tu as déjà 2 chaînes en cours de sélection. Termine-les avant d'en créer une autre."
            } else {
                errorMessage = isEditing ? "Enregistrement impossible." : "Création impossible. Vérifie ta connexion."
            }
        }
    }
}

extension ChainIntention {
    /// Libellé localisé (résout la clé via le bundle swizzlé).
    var titleKeyLocalized: String { NSLocalizedString(titleKey, comment: "") }
}

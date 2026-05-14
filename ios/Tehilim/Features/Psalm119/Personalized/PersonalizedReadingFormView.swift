import SwiftUI

/// Formulaire de génération d'une lecture personnalisée du Tehilim 119.
///
/// **Champs requis** :
/// - Prénom du proche (hébreu uniquement, validé en live)
/// - Lien : בן (fils) ou בת (fille)
/// - Prénom de la mère (hébreu uniquement)
/// - Type : Malade ou Défunt — pilote l'ajout automatique de « נשמה »
///
/// L'utilisateur **ne tape jamais « נשמה »** lui-même. C'est l'app qui l'ajoute
/// automatiquement en fin de séquence quand le type est « Défunt » (règle métier).
struct PersonalizedReadingFormView: View {
    @Environment(\.dismiss) private var dismiss

    @State private var relativeFirstName: String = ""
    @State private var relationType: RelationType = .ben
    @State private var motherFirstName: String = ""
    @State private var prayerType: PrayerType = .malade

    @State private var navigateToList = false
    @State private var generatedSequence: [ReadingLetterItem] = []

    /// Validation : les 2 prénoms doivent contenir au moins 1 lettre hébraïque.
    private var isFormValid: Bool {
        HebrewLetterMapper.isValidHebrewName(relativeFirstName) &&
        HebrewLetterMapper.isValidHebrewName(motherFirstName)
    }

    var body: some View {
        NavigationStack {
            Form {
                // MARK: - Type de prière
                Section {
                    Picker("Type", selection: $prayerType) {
                        ForEach(PrayerType.allCases) { type in
                            Text(type.labelFR).tag(type)
                        }
                    }
                    .pickerStyle(.segmented)
                } footer: {
                    if prayerType == .defunt {
                        Text("« נשמה » sera ajouté automatiquement en fin de séquence.")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                // MARK: - Proche
                Section {
                    HebrewTextField(
                        title: "Prénom",
                        placeholder: "ex. יוסף",
                        text: $relativeFirstName
                    )
                    Picker("Lien", selection: $relationType) {
                        ForEach(RelationType.allCases) { rel in
                            Text(rel.hebrew)
                                .environment(\.layoutDirection, .rightToLeft)
                                .tag(rel)
                        }
                    }
                    .pickerStyle(.segmented)
                } header: {
                    Text("Le proche concerné")
                } footer: {
                    Text("Prénom en hébreu uniquement.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                // MARK: - Mère
                Section {
                    HebrewTextField(
                        title: "Prénom de la mère",
                        placeholder: "ex. שרה",
                        text: $motherFirstName
                    )
                } header: {
                    Text("Sa mère")
                } footer: {
                    Text("Prénom en hébreu uniquement.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                // MARK: - Aperçu (calculé en live)
                if isFormValid {
                    Section {
                        Text(previewHebrew)
                            .font(.title3.weight(.medium))
                            .environment(\.layoutDirection, .rightToLeft)
                            .frame(maxWidth: .infinity, alignment: .trailing)
                            .padding(.vertical, 4)
                    } header: {
                        Text("Aperçu hébraïque")
                    }
                }
            }
            .navigationTitle("Lecture personnalisée")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Générer") {
                        generatedSequence = LetterSequenceGenerator.generate(
                            relativeName: relativeFirstName,
                            relation: relationType,
                            motherName: motherFirstName,
                            prayerType: prayerType
                        )
                        navigateToList = true
                    }
                    .disabled(!isFormValid)
                }
            }
            .navigationDestination(isPresented: $navigateToList) {
                PersonalizedReadingListView(
                    intent: SavedPrayerIntent(
                        title: LetterSequenceGenerator.makeTitle(
                            prayerType: prayerType,
                            relativeName: relativeFirstName,
                            relation: relationType,
                            motherName: motherFirstName
                        ),
                        prayerType: prayerType,
                        relativeFirstName: relativeFirstName,
                        relationType: relationType,
                        motherFirstName: motherFirstName,
                        generatedLetters: generatedSequence
                    ),
                    isSaved: false
                )
            }
        }
    }

    /// Affichage hébraïque compact en live : « יוסף בן שרה » (+ « · נשמה » si défunt).
    private var previewHebrew: String {
        var s = "\(relativeFirstName) \(relationType.hebrew) \(motherFirstName)"
        if prayerType == .defunt {
            s += " · נשמה"
        }
        return s
    }
}

// MARK: - HebrewTextField

/// `TextField` qui filtre toute saisie non-hébraïque en live.
/// Pas d'autocorrection, pas de capitalisation, layoutDirection RTL pour l'aperçu.
private struct HebrewTextField: View {
    let title: String
    let placeholder: String
    @Binding var text: String

    var body: some View {
        HStack {
            Text(title)
            Spacer()
            TextField(placeholder, text: $text)
                .multilineTextAlignment(.trailing)
                .environment(\.layoutDirection, .rightToLeft)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled(true)
                .onChange(of: text) { _, newValue in
                    let filtered = HebrewLetterMapper.filterHebrew(newValue)
                    if filtered != newValue {
                        text = filtered
                    }
                }
                .accessibilityLabel(title)
        }
    }
}

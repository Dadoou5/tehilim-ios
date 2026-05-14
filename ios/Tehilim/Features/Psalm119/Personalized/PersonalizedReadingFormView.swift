import SwiftUI
import UIKit

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
///
/// V1.10.1 : les `TextField` demandent à iOS le clavier hébreu via
/// `HebrewKeyboardTextField`. Si l'utilisateur n'a pas activé ce clavier dans ses
/// Réglages iOS, un encart d'aide s'affiche en tête du formulaire.
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
                // MARK: - Astuce clavier hébreu (si absent)
                if !UITextInputMode.isHebrewKeyboardInstalled {
                    Section {
                        keyboardHint
                    }
                }

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
                    LabeledRow(label: "Prénom") {
                        HebrewKeyboardTextField(
                            text: $relativeFirstName,
                            placeholder: "ex. יוסף"
                        )
                    }
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
                    LabeledRow(label: "Prénom de la mère") {
                        HebrewKeyboardTextField(
                            text: $motherFirstName,
                            placeholder: "ex. שרה"
                        )
                    }
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

    // MARK: - Banner clavier hébreu non installé

    @ViewBuilder
    private var keyboardHint: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "keyboard")
                .font(.title3)
                .foregroundStyle(Color.accentMain)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 4) {
                Text("Active le clavier hébreu")
                    .font(.subheadline.weight(.semibold))
                Text("Réglages → Général → Clavier → Claviers → Ajouter un clavier → Hébreu. Puis appuie sur 🌐 pendant la saisie pour basculer.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Button("Ouvrir Réglages") {
                    if let url = URL(string: UIApplication.openSettingsURLString) {
                        UIApplication.shared.open(url)
                    }
                }
                .font(.caption.weight(.medium))
                .padding(.top, 2)
            }
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
    }
}

// MARK: - LabeledRow

/// Ligne de Form avec label à gauche et `UITextField` (notre wrapper) à droite.
/// Le wrapper SwiftUI ne supporte pas bien `.fixedSize` côté UIKit, donc on
/// gère la mise en page manuellement.
private struct LabeledRow<Content: View>: View {
    let label: String
    @ViewBuilder var content: Content

    var body: some View {
        HStack(spacing: 12) {
            Text(label)
                .foregroundStyle(.primary)
            Spacer()
            content
                .frame(maxWidth: 240, alignment: .trailing)
                .frame(minHeight: 32)
        }
    }
}

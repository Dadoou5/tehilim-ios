import SwiftUI
import UIKit

/// Formulaire de génération d'une lecture personnalisée du Tehilim 119 — V1.10.2.
///
/// **Lecture exclusivement de type Lelouy Nichmat** (élévation de l'âme d'un défunt).
/// La séquence intègre automatiquement « נשמה » à la fin (règle métier).
///
/// Champs requis :
/// - Prénom du défunt (hébreu uniquement, validé en live)
/// - Lien : בן (fils) ou בת (fille)
/// - Prénom de la mère (hébreu uniquement)
///
/// L'utilisateur **ne tape jamais « נשמה »** lui-même. C'est l'app qui l'ajoute
/// automatiquement en fin de séquence.
struct PersonalizedReadingFormView: View {
    @Environment(\.dismiss) private var dismiss

    @State private var relativeFirstName: String = ""
    @State private var relationType: RelationType = .ben
    @State private var motherFirstName: String = ""

    @State private var navigateToList = false
    @State private var generatedSequence: [ReadingLetterItem] = []

    /// Type figé pour cette feature — toutes les lectures personnalisées sont
    /// des Lelouy Nichmat depuis V1.10.2 (la partie « Malade » a été retirée).
    private let prayerType: PrayerType = .defunt

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

                // MARK: - Bannière contextuelle Lelouy Nichmat
                Section {
                    lelouyBanner
                        .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                        .listRowBackground(Color.clear)
                }

                // MARK: - Défunt
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
                    Text("Le défunt")
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
                    } footer: {
                        Text("« נשמה » sera ajouté automatiquement à la fin de la séquence.")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .navigationTitle("Lelouy Nichmat")
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

    /// Affichage hébraïque compact en live : « יוסף בן שרה · נשמה ».
    private var previewHebrew: String {
        "\(relativeFirstName) \(relationType.hebrew) \(motherFirstName) · נשמה"
    }

    // MARK: - Bannière Lelouy Nichmat

    @ViewBuilder
    private var lelouyBanner: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "candle.fill")
                .font(.title3)
                .foregroundStyle(Color.accentMain)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 4) {
                Text("Lecture pour l'élévation de l'âme")
                    .font(.subheadline.weight(.semibold))
                Text("La séquence générée correspond aux lettres du prénom du défunt, du lien (בן/בת), du prénom de sa mère, puis de נשמה.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
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

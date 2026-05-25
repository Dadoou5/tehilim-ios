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
    @EnvironmentObject private var savedPrayers: SavedPrayerStore

    @State private var relativeFirstName: String = ""
    @State private var relationType: RelationType = .ben
    @State private var motherFirstName: String = ""

    @State private var navigateToList = false
    @State private var generatedIntent: SavedPrayerIntent? = nil

    // MARK: - V1.10.7 — Commémoration
    /// Date civile du décès (optionnelle). Si renseignée et `remindersEnabled`,
    /// déclenche le scheduling des notifications J-7 / jour J.
    @State private var civilDateOfDeath: Date? = nil
    @State private var remindersEnabled: Bool = false
    @State private var notifySevenDaysBefore: Bool = true
    @State private var notifySameDay: Bool = true
    /// Statut de la permission notif système — pour afficher le hint si denied.
    @ObservedObject private var notifications = NotificationManager.shared

    /// V1.10.7 — focus chain entre les deux champs hébreux.
    ///
    /// Un seul `@State` enum (pas deux Bool indépendants) garantit l'**exclusivité
    /// mutuelle** : impossible que les deux champs soient marqués focused
    /// simultanément. Sans ça, les sync async via `textFieldDid…Editing`
    /// créaient une fenêtre de race où le UITextField source se ré-arrogeait
    /// le firstResponder → les keystrokes étaient répliqués sur les deux
    /// champs (bug remonté en test).
    private enum FocusedField { case relative, mother }
    @State private var focusedField: FocusedField? = nil

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
                        // V1.10.7 — returnKey `.next`. Le Binding dérivé
                        // de `focusedField` garantit qu'à tout moment, AU
                        // PLUS UN seul champ est focused.
                        HebrewKeyboardTextField(
                            text: $relativeFirstName,
                            placeholder: "ex. יוסף",
                            returnKeyType: .next,
                            isFocused: focusBinding(for: .relative),
                            onReturn: { focusedField = .mother }
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
                        // V1.10.7 — returnKey `.done`. onReturn met
                        // `focusedField = nil` → resignFirstResponder, le
                        // clavier se ferme.
                        HebrewKeyboardTextField(
                            text: $motherFirstName,
                            placeholder: "ex. שרה",
                            returnKeyType: .done,
                            isFocused: focusBinding(for: .mother),
                            onReturn: { focusedField = nil }
                        )
                    }
                } header: {
                    Text("Sa mère")
                } footer: {
                    Text("Prénom en hébreu uniquement.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                // MARK: - V1.10.7 — Commémoration (section optionnelle)
                Section {
                    // Date du décès (date civile, optionnelle)
                    DatePicker(
                        "Date du décès",
                        selection: Binding(
                            get: { civilDateOfDeath ?? Date() },
                            set: { civilDateOfDeath = $0 }
                        ),
                        displayedComponents: .date
                    )
                    if civilDateOfDeath != nil {
                        Button("Effacer la date", role: .destructive) {
                            civilDateOfDeath = nil
                        }
                    }

                    // Toggle global "Recevoir un rappel"
                    Toggle("Recevoir un rappel", isOn: $remindersEnabled)
                        .onChange(of: remindersEnabled) { _, newValue in
                            if newValue {
                                Task { _ = await notifications.requestPermission() }
                            }
                        }

                    if remindersEnabled {
                        Toggle("7 jours avant", isOn: $notifySevenDaysBefore)
                        Toggle("Le jour même", isOn: $notifySameDay)
                    }

                    // Aperçu de la prochaine azcara (calculée en live)
                    if let death = civilDateOfDeath,
                       let next = MemorialCalculator.nextYahrzeit(deathCivil: death) {
                        HStack {
                            Text("Prochaine azcara")
                                .foregroundStyle(.secondary)
                            Spacer()
                            // Astérisque expliqué dans le footer de section.
                            Text("\(next.formatted(date: .abbreviated, time: .omitted))*")
                                .foregroundStyle(Color.accentMain)
                        }
                    }
                } header: {
                    Text("Commémoration")
                } footer: {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("L'application calcule automatiquement la prochaine azcara à partir de la date civile saisie.")
                        if civilDateOfDeath != nil {
                            Text("* commence la veille au soir")
                        }
                        if remindersEnabled && civilDateOfDeath == nil {
                            Text("Ajoutez une date du décès pour programmer les rappels.")
                                .foregroundStyle(.orange)
                        }
                        if remindersEnabled && notifications.permission == .denied {
                            Text("Les notifications sont désactivées sur cet appareil.")
                                .foregroundStyle(.orange)
                        }
                    }
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
                        generateAndSave()
                    }
                    .disabled(!isFormValid)
                }
            }
            .navigationDestination(isPresented: $navigateToList) {
                if let intent = generatedIntent {
                    // V1.10.5 : isSaved=true → l'intent est déjà persisté (sync iCloud).
                    PersonalizedReadingListView(intent: intent, isSaved: true)
                }
            }
        }
    }

    /// Affichage hébraïque compact en live : « יוסף בן שרה · נשמה ».
    private var previewHebrew: String {
        "\(relativeFirstName) \(relationType.hebrew) \(motherFirstName) · נשמה"
    }

    /// V1.10.7 — convertit l'enum `focusedField` en `Binding<Bool>` pour
    /// chacun des champs. Le setter ne nilifie l'enum que si on désélectionne
    /// LE champ correspondant (pas un autre), évitant les races entre
    /// les didEndEditing async des deux champs.
    private func focusBinding(for field: FocusedField) -> Binding<Bool> {
        Binding(
            get: { focusedField == field },
            set: { isOn in
                if isOn {
                    focusedField = field
                } else if focusedField == field {
                    focusedField = nil
                }
            }
        )
    }

    /// V1.10.5 — Génère la séquence ET sauvegarde automatiquement.
    /// Dédup : si un Lelouy Nichmat avec exactement les mêmes paramètres existe
    /// déjà, on le réutilise au lieu de créer un doublon (utile si l'utilisateur
    /// re-tape « Générer » sur le même formulaire).
    ///
    /// V1.10.7 — embarque les champs Commémoration et schedule les rappels.
    private func generateAndSave() {
        let sequence = LetterSequenceGenerator.generate(
            relativeName: relativeFirstName,
            relation: relationType,
            motherName: motherFirstName,
            prayerType: prayerType
        )
        let candidate = SavedPrayerIntent(
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
            generatedLetters: sequence,
            civilDateOfDeath: civilDateOfDeath,
            hebrewDateOfDeath: civilDateOfDeath.map { MemorialCalculator.hebrewYMD(from: $0) },
            remindersEnabled: remindersEnabled,
            notifySevenDaysBefore: notifySevenDaysBefore,
            notifySameDay: notifySameDay
        )
        // Add ou retourne l'existant (dédup) — déclenche aussi la sync iCloud.
        let saved = savedPrayers.addOrFindExisting(candidate)
        generatedIntent = saved
        // V1.10.7 — schedule (ou no-op si conditions pas réunies, géré dans
        // NotificationManager.rescheduleMemorialReminders).
        // **AWAIT avant de naviguer** : sinon PersonalizedReadingListView
        // s'affiche AVANT que UNUserNotificationCenter ait enregistré la
        // requête → `pendingMemorialReminders` retourne tableau vide et la
        // section « Rappels programmés » reste cachée.
        Task {
            await notifications.rescheduleMemorialReminders(for: saved)
            navigateToList = true
        }
    }

    // MARK: - Bannière Lelouy Nichmat

    @ViewBuilder
    private var lelouyBanner: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "flame.fill")
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

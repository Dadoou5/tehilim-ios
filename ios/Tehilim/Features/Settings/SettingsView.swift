import SwiftUI
import WidgetKit

struct SettingsView: View {
    @Binding var path: NavigationPath
    @StateObject private var prefs = Preferences()
    @ObservedObject private var notifications = NotificationManager.shared
    @State private var showRestartAlert = false

    var body: some View {
        NavigationStack(path: $path) {
            Form {
                Section {
                    Picker("Mode du texte", selection: $prefs.textMode) {
                        ForEach(TextMode.allCases) { Text($0.label).tag($0) }
                    }
                    Picker("Taille hébreu / phonétique", selection: $prefs.textSizeHebrew) {
                        ForEach(TextSize.allCases) { Text($0.label).tag($0) }
                    }
                    Picker("Taille français", selection: $prefs.textSizeFR) {
                        ForEach(TextSize.allCases) { Text($0.label).tag($0) }
                    }

                    PrimaryPreviewRow(mode: prefs.textMode, size: prefs.textSizeHebrew)
                    FrenchPreviewRow(size: prefs.textSizeFR)
                } header: {
                    Text("Texte du psaume")
                } footer: {
                    if prefs.textMode == .phonetic {
                        Text("Transcription assistée du texte hébreu en lettres latines (sépharade). Approximation algorithmique, ne remplace pas une lecture du texte original.")
                            .font(.caption)
                    }
                }

                Section {
                    Picker("Langue", selection: $prefs.appLanguage) {
                        Text("Système").tag(AppLanguage.system)
                        Text("Français").tag(AppLanguage.fr)
                        Text("English").tag(AppLanguage.en)
                    }
                    .onChange(of: prefs.appLanguage) { _, _ in
                        // Écrit AppleLanguages tout de suite ; effet UI au prochain démarrage.
                        TehilimApp.applyLanguagePreference()
                        showRestartAlert = true
                    }
                    Toggle("Afficher la traduction par défaut", isOn: $prefs.translationFR)
                } header: {
                    Text("Langue")
                } footer: {
                    Text("Source : \(prefs.appLanguage.translation.sourceCredit)")
                        .font(.caption)
                }

                Section("Affichage") {
                    Picker("Thème", selection: $prefs.theme) {
                        ForEach(AppTheme.allCases) { Text($0.label).tag($0) }
                    }
                    Picker("Numérotation des versets", selection: $prefs.verseNumberStyle) {
                        ForEach(VerseNumberStyle.allCases) { Text($0.label).tag($0) }
                    }
                }

                Section("Lecture quotidienne") {
                    Picker("Mode", selection: $prefs.dailyMode) {
                        Text("Cycle mensuel").tag(DailyMode.monthly)
                        Text("Jour de la semaine").tag(DailyMode.weekly)
                    }
                    .onChange(of: prefs.dailyMode) { _, _ in
                        // Force le widget à recharger sa timeline avec le nouveau mode.
                        WidgetCenter.shared.reloadAllTimelines()
                    }
                }

                NotificationsSection(prefs: prefs, notifications: notifications)

                Section("Accessibilité") {
                    NavigationLink("Déclaration d'accessibilité") {
                        AccessibilityDeclarationView()
                    }
                }

                Section("À propos") {
                    NavigationLink("Sources du contenu") { AboutContentView() }
                    NavigationLink("Confidentialité") { AboutPrivacyView() }
                    HStack {
                        Text("Version")
                        Spacer()
                        Text(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0")
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .appBackground()
            .navigationTitle("Réglages")
            .alert("Redémarrage requis", isPresented: $showRestartAlert) {
                Button("OK", role: .cancel) { }
            } message: {
                Text("La traduction des Tehilim a basculé immédiatement. Pour que l'interface change aussi, ferme l'app puis rouvre-la.")
            }
        }
    }
}

// MARK: - Notifications

private struct NotificationsSection: View {
    @ObservedObject var prefs: Preferences
    @ObservedObject var notifications: NotificationManager

    var body: some View {
        Section {
            Toggle("Me notifier chaque jour", isOn: $prefs.notificationEnabled)
                .onChange(of: prefs.notificationEnabled) { _, enabled in
                    Task { await handleToggle(enabled) }
                }

            if prefs.notificationEnabled {
                DatePicker(
                    "Heure du rappel",
                    selection: timeBinding,
                    displayedComponents: .hourAndMinute
                )

                if notifications.permission == .denied {
                    DeniedBanner(notifications: notifications)
                }
            }
        } header: {
            Text("Rappel quotidien")
        } footer: {
            if prefs.notificationEnabled, notifications.permission != .denied {
                Text("Tu recevras un rappel chaque jour à cette heure pour les Tehilim du jour.")
                    .font(.caption)
            }
        }
    }

    // MARK: - Bindings

    private var timeBinding: Binding<Date> {
        Binding(
            get: {
                var c = DateComponents()
                c.hour = prefs.notificationHour
                c.minute = prefs.notificationMinute
                return Calendar.current.date(from: c) ?? Date()
            },
            set: { newDate in
                let c = Calendar.current.dateComponents([.hour, .minute], from: newDate)
                prefs.notificationHour = c.hour ?? 8
                prefs.notificationMinute = c.minute ?? 0
                Task {
                    await notifications.scheduleDailyReminder(
                        hour: prefs.notificationHour,
                        minute: prefs.notificationMinute
                    )
                }
            }
        )
    }

    // MARK: - Logic

    private func handleToggle(_ enabled: Bool) async {
        if enabled {
            await notifications.refreshPermission()
            switch notifications.permission {
            case .notDetermined:
                let granted = await notifications.requestPermission()
                if granted {
                    await notifications.scheduleDailyReminder(
                        hour: prefs.notificationHour,
                        minute: prefs.notificationMinute
                    )
                } else {
                    await MainActor.run { prefs.notificationEnabled = false }
                }
            case .authorized, .provisional, .ephemeral:
                await notifications.scheduleDailyReminder(
                    hour: prefs.notificationHour,
                    minute: prefs.notificationMinute
                )
            case .denied:
                // On garde le toggle ON pour que la bannière s'affiche.
                break
            @unknown default:
                break
            }
        } else {
            await notifications.cancelDailyReminder()
        }
    }
}

private struct DeniedBanner: View {
    @ObservedObject var notifications: NotificationManager

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label("Notifications désactivées dans iOS", systemImage: "exclamationmark.triangle")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Color.errorToken)
            Text("Active-les dans Réglages iOS → Tehilim → Notifications pour recevoir le rappel.")
                .font(.caption)
                .foregroundStyle(.secondary)
            Button("Ouvrir Réglages iOS") {
                notifications.openSystemSettings()
            }
            .buttonStyle(.bordered)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Aperçus texte

private struct PrimaryPreviewRow: View {
    let mode: TextMode
    let size: TextSize
    private let sample = "שִׁיר לַמַּעֲלוֹת אֶשָּׂא עֵינַי"
    var body: some View {
        VStack(alignment: mode == .hebrew ? .trailing : .leading, spacing: 4) {
            Text("Aperçu").font(.caption2).foregroundStyle(.secondary)
            Group {
                if mode == .hebrew {
                    Text(sample)
                        .multilineTextAlignment(.trailing)
                        .environment(\.layoutDirection, .rightToLeft)
                } else {
                    Text(HebrewTransliterator.transliterate(sample))
                        .italic()
                        .multilineTextAlignment(.leading)
                }
            }
            .font(.hebrewBody(size))
            .frame(maxWidth: .infinity,
                   alignment: mode == .hebrew ? .trailing : .leading)
            .lineSpacing(mode == .hebrew ? 8 : 4)
        }
        .padding(.vertical, 4)
    }
}

private struct FrenchPreviewRow: View {
    let size: TextSize
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Aperçu traduction").font(.caption2).foregroundStyle(.secondary)
            Text("Cantique des degrés. Je lève mes yeux vers les montagnes…")
                .font(.frBody(size))
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, 4)
    }
}

private struct AboutContentView: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Group {
                    Text("Texte hébreu").font(.headline).accessibilityAddTraits(.isHeader)
                    Text("Source : Sefaria — édition « Miqra according to the Masorah ». Texte massorétique avec nikud, sans téamim. Domaine public.")
                }
                Group {
                    Text("Traduction française").font(.headline).accessibilityAddTraits(.isHeader)
                    Text("Beth Loubavitch — le-tehilim.online")
                        .font(.subheadline.weight(.medium))
                    Text("8 rue Lamartine, 75009 Paris\nchabad@loubavitch.fr")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("Utilisée avec l'autorisation expresse de l'éditeur.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                Group {
                    Text("English translation").font(.headline).accessibilityAddTraits(.isHeader)
                    Text("Sefaria — JPS 1917 (The Holy Scriptures: A New Translation)")
                        .font(.subheadline.weight(.medium))
                    Text("Domaine public — publié à l'origine par la Jewish Publication Society of America en 1917.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                Divider()
                Text("Aucune donnée personnelle n'est collectée par cette application.")
                    .foregroundStyle(.secondary)
            }
            .padding()
        }
        .navigationTitle("Sources")
    }
}

private struct AboutPrivacyView: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("Cette application ne collecte aucune donnée personnelle.")
                Text("Toutes les préférences sont stockées localement sur l'appareil.")
                Text("Aucune connexion réseau n'est nécessaire au fonctionnement principal.")
            }
            .padding()
        }
        .navigationTitle("Confidentialité")
    }
}

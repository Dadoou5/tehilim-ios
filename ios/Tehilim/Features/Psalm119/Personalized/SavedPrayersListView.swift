import SwiftUI

/// Liste des Lelouy Nichmat sauvegardés — V1.10.2 (section unique).
///
/// Note compatibilité : les anciennes entrées de type `.malade` (V1.10.0/1) sont
/// affichées si elles existent, sans rupture. Le store n'efface jamais les données.
struct SavedPrayersListView: View {
    @EnvironmentObject private var savedPrayers: SavedPrayerStore

    var body: some View {
        Group {
            if savedPrayers.intents.isEmpty {
                EmptyStateView(
                    symbol: "tray",
                    title: "Aucun Lelouy Nichmat sauvegardé",
                    message: "Génère une lecture personnalisée et sauvegarde-la pour la retrouver ici."
                )
            } else {
                List {
                    Section {
                        ForEach(sortedIntents) { intent in
                            NavigationLink(destination: PersonalizedReadingListView(intent: intent, isSaved: true)) {
                                row(intent)
                            }
                        }
                        .onDelete(perform: deleteFromSorted)
                    } header: {
                        Text("Lelouy Nichmat")
                    } footer: {
                        // V1.10.7 — pas de conditionnel `s` dans
                        // l'interpolation (casserait la clé de lookup
                        // LocalizedStringKey). On choisit la clé selon
                        // le count, chaque clé existe en EN.
                        let count = savedPrayers.intents.count
                        VStack(alignment: .leading, spacing: 6) {
                            Group {
                                if count <= 1 {
                                    Text("\(count) sauvegardé")
                                } else {
                                    Text("\(count) sauvegardés")
                                }
                            }
                            // Explication de l'astérisque affichée à côté
                            // de la prochaine azcara — affichée si au moins
                            // une prière a une date du décès renseignée.
                            if sortedIntents.contains(where: { $0.civilDateOfDeath != nil }) {
                                Text("* commence la veille au soir")
                            }
                        }
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                    }
                }
                .listStyle(.insetGrouped)
                .appBackground()
            }
        }
        .navigationTitle("Mes prières")
        .navigationBarTitleDisplayMode(.large)
        // V1.10.7 — EditButton dans la toolbar : rend la suppression
        // découvrable pour les nouveaux users (qui ne pensent pas
        // spontanément au swipe). Pattern natif iOS — utilisé par Mail,
        // Notes, Rappels. Le swipe-to-delete continue de fonctionner en
        // parallèle pour les power users.
        .toolbar {
            if !savedPrayers.intents.isEmpty {
                ToolbarItem(placement: .navigationBarTrailing) {
                    EditButton()
                }
            }
        }
    }

    @ViewBuilder
    private func row(_ intent: SavedPrayerIntent) -> some View {
        // V1.10.7 — wrap row content in HStack pour rendre place à une
        // icône cloche en haut-droite quand les rappels sont actifs.
        // VStack original conservé pour ne pas perturber la lecture.
        HStack(alignment: .top, spacing: 8) {
            VStack(alignment: .leading, spacing: 4) {
                Text(intent.hebrewSubject)
                    .font(.headline)
                    .environment(\.layoutDirection, .rightToLeft)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                HStack(spacing: 8) {
                    Text("\(intent.generatedLetters.count) lettres")
                    Text("·")
                    Text(intent.createdAt.formatted(date: .abbreviated, time: .omitted))
                    if let last = intent.lastReadIndex {
                        Text("·")
                        Text("lue jusqu'à \(last + 1)")
                            .foregroundStyle(Color.accentMain)
                    }
                }
                .font(.caption)
                .foregroundStyle(.secondary)

                // V1.10.7 — Prochaine azcara si la date du décès est renseignée.
                // Astérisque rappelle que le Hebrew day commence au coucher
                // du soleil de la veille civile (cf. footer de la section).
                if let death = intent.civilDateOfDeath,
                   let next = MemorialCalculator.nextYahrzeit(deathCivil: death) {
                    HStack(spacing: 6) {
                        Image(systemName: "flame.fill")
                            .font(.caption2)
                        Text("Prochaine azcara")
                        Text(":")
                        Text("\(next.formatted(date: .abbreviated, time: .omitted))*")
                            .fontWeight(.medium)
                    }
                    .font(.caption)
                    .foregroundStyle(Color.accentMain)
                }
            }

            Spacer(minLength: 0)

            // V1.10.7 — Badge cloche si rappels effectivement planifiés.
            // Conditions strictes (= mêmes que le scheduler) pour ne pas
            // induire en erreur l'utilisateur.
            if hasActiveReminders(intent) {
                Image(systemName: "bell.fill")
                    .font(.caption)
                    .foregroundStyle(Color.accentMain)
                    .accessibilityLabel("Rappels activés")
                    .padding(.top, 4)
            }
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
    }

    /// V1.10.7 — Réplique exactement les conditions du scheduler
    /// (`NotificationManager.rescheduleMemorialReminders`) : on n'affiche
    /// la cloche que si une notif sera réellement posée.
    private func hasActiveReminders(_ intent: SavedPrayerIntent) -> Bool {
        intent.remindersEnabled
            && intent.civilDateOfDeath != nil
            && (intent.notifySevenDaysBefore || intent.notifySameDay)
    }

    private func delete(at offsets: IndexSet) {
        savedPrayers.delete(at: offsets)
    }

    /// V1.10.7 — Mes prières triées par prochaine azcara croissante
    /// (les commémorations à venir en premier), puis par date de création
    /// décroissante pour les prières sans date du décès renseignée.
    private var sortedIntents: [SavedPrayerIntent] {
        let now = Date()
        let withAzcara = savedPrayers.intents.compactMap { intent -> (SavedPrayerIntent, Date)? in
            guard let death = intent.civilDateOfDeath,
                  let next = MemorialCalculator.nextYahrzeit(deathCivil: death, now: now)
            else { return nil }
            return (intent, next)
        }
        .sorted { lhs, rhs in
            if lhs.1 != rhs.1 { return lhs.1 < rhs.1 }
            // Tiebreaker : créé en dernier en premier (= comportement
            // historique pour les prières insérées le même jour avec
            // la même date du décès).
            return lhs.0.createdAt > rhs.0.createdAt
        }
        .map { $0.0 }

        let withoutAzcara = savedPrayers.intents
            .filter { $0.civilDateOfDeath == nil }
            .sorted { $0.createdAt > $1.createdAt }

        return withAzcara + withoutAzcara
    }

    /// Mappe les offsets du `ForEach(sortedIntents)` vers les indices du
    /// store sous-jacent (qui peut être dans un ordre différent).
    /// V1.10.7 — annule aussi les rappels d'azcara associés à la prière
    /// supprimée, sinon les UNNotificationRequest restent en file et
    /// déclenchent des notifs orphelines.
    private func deleteFromSorted(at offsets: IndexSet) {
        let toDelete = offsets.map { sortedIntents[$0] }
        for intent in toDelete {
            let intentId = intent.id
            Task { await NotificationManager.shared.cancelMemorialReminders(intentId: intentId) }
            savedPrayers.delete(intent)
        }
    }
}

import SwiftUI
import WidgetKit

struct DailyView: View {
    @Binding var path: NavigationPath
    @EnvironmentObject private var container: AppContainer
    @StateObject private var prefs = Preferences()
    @State private var modeSheet = false
    @State private var presentedPrayer: Prayer.Kind? = nil

    var body: some View {
        NavigationStack(path: $path) {
            List {
                Section {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(prefs.dailyMode.label).font(.headline)
                            Text(modeSubtitle).font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Button("Changer") { modeSheet = true }
                    }
                }

                Section {
                    Button {
                        presentedPrayer = .before
                    } label: {
                        Label("Prière avant la lecture", systemImage: Prayer.Kind.before.symbol)
                    }
                }

                Section {
                    let psalms = container.dailyEngine.psalmsForToday(mode: prefs.dailyMode)
                    if psalms.isEmpty {
                        Text("Aucun Tehilim défini pour ce jour.")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(psalms, id: \.self) { id in
                            if let p = container.psalmRepository.psalm(id: id) {
                                NavigationLink(destination: PsalmDetailView(psalmId: id, siblings: psalms)) {
                                    HStack {
                                        Text("Tehilim \(p.id) · \(p.hebrewNumber)")
                                        Spacer()
                                    }
                                }
                            }
                        }
                    }
                } header: {
                    Text("Au programme")
                }

                Section {
                    Button {
                        presentedPrayer = .after
                    } label: {
                        Label("Prière après la lecture", systemImage: Prayer.Kind.after.symbol)
                    }
                }
            }
            .listStyle(.insetGrouped)
            .appBackground()
            .navigationTitle("Aujourd'hui")
            .sheet(isPresented: $modeSheet) {
                DailyModeSheet().presentationDetents([.medium])
            }
            .sheet(item: $presentedPrayer) { kind in
                PrayerView(prayer: Prayer.of(kind))
            }
        }
    }

    private var modeSubtitle: String {
        switch prefs.dailyMode {
        case .monthly:
            var cal = Calendar(identifier: .hebrew)
            cal.timeZone = .current
            return "Cycle mensuel — jour \(cal.component(.day, from: Date()))"
        case .weekly:
            return "Jour de la semaine"
        case .custom:
            return "Personnalisé (V2)"
        }
    }
}

private struct DailyModeSheet: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var prefs = Preferences()
    var body: some View {
        NavigationStack {
            Form {
                Picker("Mode", selection: $prefs.dailyMode) {
                    Text("Cycle mensuel").tag(DailyMode.monthly)
                    Text("Jour de la semaine").tag(DailyMode.weekly)
                }
                .pickerStyle(.inline)
                .onChange(of: prefs.dailyMode) { _, _ in
                    WidgetCenter.shared.reloadAllTimelines()
                }
            }
            .appBackground()
            .navigationTitle("Mode de lecture")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("OK") { dismiss() }
                }
            }
        }
    }
}

import Foundation
import Combine
import CoreLocation
import WidgetKit

/// Pilote le mode Chabbat côté app : résout la position (GPS « pendant
/// l'utilisation », repli sur une ville choisie dans Réglages), calcule l'état
/// Chabbat via `ShabbatCalculator`, le publie pour l'UI, l'écrit dans l'App
/// Group pour le widget, et re-planifie une réévaluation à la sortie de Chabbat.
///
/// Échappatoire : `continueAnyway()` lève le blocage pour la session courante
/// (réinitialisé au prochain lancement, ou automatiquement à la fin de Chabbat).
final class ShabbatManager: NSObject, ObservableObject {

    @Published private(set) var state = ShabbatState(isShabbat: false, endsAt: nil, nextStartsAt: nil)
    @Published private(set) var resolvedCoordinate: GeoCoordinate?
    @Published private(set) var usingGPS = false
    /// L'utilisateur a tapé « continuer quand même » → on ne bloque plus
    /// jusqu'au prochain lancement (ou la fin de Chabbat).
    @Published private(set) var overriddenThisSession = false

    private let preferences: Preferences
    private let locationManager = CLLocationManager()
    private var gpsCoordinate: GeoCoordinate?
    private var timer: Timer?

    /// True quand l'app doit afficher l'écran Chabbat Chalom (mode activé +
    /// en Chabbat OU pré-Chabbat + pas d'override de session).
    var isBlocking: Bool {
        preferences.shabbatModeEnabled && state.shouldDisplay && !overriddenThisSession
    }

    /// Phase courante — sert à réinitialiser l'override au changement de phase
    /// (ex. l'utilisateur fait « continuer » en pré-Chabbat → l'écran doit
    /// réapparaître à l'entrée réelle de Chabbat).
    private enum Phase { case none, pre, shabbat }
    private var lastPhase: Phase = .none

    init(preferences: Preferences) {
        self.preferences = preferences
        super.init()
        locationManager.delegate = self
        // Précision « ville » suffisante (faible conso, compatible localisation
        // approximative).
        locationManager.desiredAccuracy = kCLLocationAccuracyThreeKilometers
        refresh()
    }

    /// Lève le blocage pour la session courante.
    func continueAnyway() { overriddenThisSession = true }

    /// Une seule demande de localisation par session (évite de re-demander à
    /// chaque retour au premier plan).
    private var didRequestLocation = false

    /// Recalcule l'état — à appeler à l'apparition et au retour au premier plan.
    func refresh() {
        if preferences.shabbatModeEnabled { requestLocationIfNeeded() }
        recompute()
    }

    // MARK: - Localisation

    private func requestLocationIfNeeded() {
        guard !didRequestLocation else { return }
        switch locationManager.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            didRequestLocation = true
            locationManager.requestLocation()
        case .notDetermined:
            didRequestLocation = true
            // **Différé** : l'alerte système de localisation, présentée
            // immédiatement au lancement, EMPÊCHE la feuille d'import de
            // prière de s'afficher (iOS interdit deux présentations
            // simultanées). On laisse donc l'app se poser (~3,5 s) : une
            // éventuelle feuille d'import apparaît d'abord, puis l'alerte se
            // présente par-dessus. Le repli ville assure le calcul entre-temps.
            DispatchQueue.main.asyncAfter(deadline: .now() + 3.5) { [weak self] in
                self?.locationManager.requestWhenInUseAuthorization()
            }
        default:
            break // refusé/restreint → repli ville
        }
    }

    /// Position retenue : GPS si dispo, sinon ville choisie, sinon Paris.
    private func resolvedCoord() -> GeoCoordinate? {
        if let gps = gpsCoordinate { return gps }
        if !preferences.shabbatCityId.isEmpty,
           let c = ShabbatCalculator.city(id: preferences.shabbatCityId) {
            return c.coordinate
        }
        return ShabbatCalculator.city(id: "paris")?.coordinate
    }

    // MARK: - Calcul

    private func recompute() {
        let coord = resolvedCoord()
        resolvedCoordinate = coord
        usingGPS = (gpsCoordinate != nil)

        guard let coord else {
            state = ShabbatState(isShabbat: false, endsAt: nil, nextStartsAt: nil)
            return
        }
        let s = ShabbatCalculator.state(now: Date(), coordinate: coord)
        state = s
        // Au changement de phase (aucune → pré → Chabbat → aucune), on réarme
        // l'override : « continuer » en pré-Chabbat ne dispense pas du blocage
        // à l'entrée réelle, et tout se réinitialise après la sortie.
        let phase: Phase = s.isShabbat ? .shabbat : (s.isPreShabbat ? .pre : .none)
        if phase != lastPhase {
            overriddenThisSession = false
            lastPhase = phase
        }
        writeToAppGroup(coord: coord)
        scheduleReevaluation(for: s)
    }

    private func writeToAppGroup(coord: GeoCoordinate) {
        let d = AppGroup.userDefaults
        d.set(preferences.shabbatModeEnabled, forKey: AppGroup.Keys.shabbatEnabled)
        d.set(coord.latitude, forKey: AppGroup.Keys.shabbatLatitude)
        d.set(coord.longitude, forKey: AppGroup.Keys.shabbatLongitude)
        WidgetCenter.shared.reloadAllTimelines()
    }

    /// Programme un recalcul à la bascule suivante (fin de Chabbat, ou prochain
    /// allumage des bougies) pour rafraîchir l'écran sans intervention.
    private func scheduleReevaluation(for s: ShabbatState) {
        timer?.invalidate()
        // En affichage (pré-Chabbat/Chabbat) → réveil à la havdala ; sinon →
        // à la prochaine apparition de l'écran (entrée − 1 h).
        let fire = s.shouldDisplay ? s.endsAt : s.nextStartsAt
        guard let fire else { return }
        let interval = max(30, fire.timeIntervalSinceNow + 2)
        // Cap à ~6h pour éviter un timer trop long invalidé en arrière-plan ;
        // refresh() au retour foreground couvre le reste.
        let capped = min(interval, 6 * 3600)
        timer = Timer.scheduledTimer(withTimeInterval: capped, repeats: false) { [weak self] _ in
            self?.recompute()
        }
    }
}

extension ShabbatManager: CLLocationManagerDelegate {
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        switch manager.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            manager.requestLocation()
        default:
            recompute()
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let loc = locations.last else { return }
        gpsCoordinate = GeoCoordinate(latitude: loc.coordinate.latitude,
                                      longitude: loc.coordinate.longitude)
        recompute()
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        recompute() // garde le repli ville
    }
}

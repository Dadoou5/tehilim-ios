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
    /// en Chabbat + pas d'override de session).
    var isBlocking: Bool {
        preferences.shabbatModeEnabled && state.isShabbat && !overriddenThisSession
    }

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

    /// Recalcule l'état — à appeler à l'apparition et au retour au premier plan.
    func refresh() {
        if preferences.shabbatModeEnabled { requestLocationIfNeeded() }
        recompute()
    }

    // MARK: - Localisation

    private func requestLocationIfNeeded() {
        switch locationManager.authorizationStatus {
        case .notDetermined:
            locationManager.requestWhenInUseAuthorization()
        case .authorizedWhenInUse, .authorizedAlways:
            locationManager.requestLocation()
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
        // Chabbat terminé → on réarme l'override pour le prochain Chabbat.
        if !s.isShabbat { overriddenThisSession = false }
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
        let fire = s.isShabbat ? s.endsAt : s.nextStartsAt
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

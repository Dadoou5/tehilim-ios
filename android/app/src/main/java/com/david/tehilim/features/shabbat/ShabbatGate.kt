package com.david.tehilim.features.shabbat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.david.tehilim.AppContainer
import com.david.tehilim.core.service.GeoCoordinate
import com.david.tehilim.core.service.ShabbatCalculator
import com.david.tehilim.core.service.ShabbatState
import kotlinx.coroutines.delay
import java.util.Date

/** État résolu du mode Chabbat pour piloter l'UI. */
data class ShabbatGate(
    val isBlocking: Boolean,
    val endsAt: Date?,
    val onContinue: () -> Unit
)

/**
 * Résout l'état Chabbat côté app : position (GPS coarse, repli ville/Paris),
 * calcul via [ShabbatCalculator], échappatoire de session, et persistance de
 * la position pour le widget. Mirror du ShabbatManager.swift.
 */
@Composable
fun rememberShabbatGate(container: AppContainer): ShabbatGate {
    val context = LocalContext.current
    val enabled by container.preferences.shabbatEnabled.collectAsState(initial = true)
    val cityId by container.preferences.shabbatCityId.collectAsState(initial = "")

    var coordinate by remember { mutableStateOf<GeoCoordinate?>(null) }
    var overridden by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(hasCoarseLocation(context)) }
    // Tick recalculé chaque minute pour basculer à l'allumage / la Havdala.
    var nowTick by remember { mutableStateOf(System.currentTimeMillis()) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    // Demande la localisation une fois si le mode est activé et qu'aucune
    // ville n'est choisie (sinon on respecte le choix ville, sans pop-up).
    LaunchedEffect(enabled, cityId) {
        if (enabled && !hasPermission && cityId.isEmpty()) {
            permLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    // Résout la coordonnée et la mémorise pour le widget.
    LaunchedEffect(enabled, cityId, hasPermission, nowTick) {
        val resolved = resolveCoordinate(context, hasPermission, cityId)
        coordinate = resolved
        if (resolved != null) {
            container.preferences.setShabbatLocation(resolved.latitude, resolved.longitude)
        }
    }

    // Recalcule chaque minute.
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            nowTick = System.currentTimeMillis()
        }
    }

    val state: ShabbatState = remember(enabled, coordinate, nowTick) {
        val coord = coordinate
        if (!enabled || coord == null) ShabbatState(false, null, null)
        else ShabbatCalculator.state(Date(), coord)
    }

    // Réarme l'override à la sortie de Chabbat.
    LaunchedEffect(state.isShabbat) { if (!state.isShabbat) overridden = false }

    return ShabbatGate(
        isBlocking = enabled && state.isShabbat && !overridden,
        endsAt = state.endsAt,
        onContinue = { overridden = true }
    )
}

private fun hasCoarseLocation(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/** GPS (dernière position connue) si autorisé, sinon ville choisie, sinon Paris. */
private fun resolveCoordinate(
    context: Context,
    hasPermission: Boolean,
    cityId: String
): GeoCoordinate? {
    if (hasPermission) {
        runCatching {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            for (p in providers) {
                if (lm?.isProviderEnabled(p) == true) {
                    val loc = lm.getLastKnownLocation(p)
                    if (loc != null) return GeoCoordinate(loc.latitude, loc.longitude)
                }
            }
        }
    }
    if (cityId.isNotEmpty()) ShabbatCalculator.city(cityId)?.let { return it.coordinate }
    return ShabbatCalculator.city("paris")?.coordinate
}

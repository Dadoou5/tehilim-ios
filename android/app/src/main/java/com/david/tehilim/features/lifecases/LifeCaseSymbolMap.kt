package com.david.tehilim.features.lifecases

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Looks8
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Mapping SF Symbols iOS (champ `symbol` dans life_cases.json) → Material Icons.
 * On garde l'intention visuelle (avion = avion, cœur = cœur).
 */
object LifeCaseSymbolMap {

    fun iconFor(symbol: String): ImageVector = when (symbol) {
        "figure.2.and.child.holdinghands" -> Icons.Outlined.ChildCare
        "figure.and.child.holdinghands"   -> Icons.Outlined.ChildCare
        "person.2.fill"                    -> Icons.Outlined.People
        "heart.fill"                       -> Icons.Outlined.Favorite
        "heart.text.square"                -> Icons.Outlined.HealthAndSafety
        "leaf", "leaf.fill"                -> Icons.Outlined.Spa
        "sparkles"                         -> Icons.Outlined.AutoAwesome
        "sun.max.fill"                     -> Icons.Outlined.WbSunny
        "moon.stars.fill"                  -> Icons.Outlined.Nightlight
        "airplane"                         -> Icons.Outlined.Flight
        "shield.fill"                      -> Icons.Outlined.Security
        "wind"                             -> Icons.Outlined.Air
        "briefcase.fill"                   -> Icons.Outlined.Work
        "flag.fill"                        -> Icons.Outlined.Flag
        "scalemass"                        -> Icons.Outlined.Balance
        "8.circle.fill"                    -> Icons.Outlined.Looks8
        "arrow.uturn.backward.circle"      -> Icons.Outlined.Restore
        else                               -> Icons.Outlined.Favorite
    }
}

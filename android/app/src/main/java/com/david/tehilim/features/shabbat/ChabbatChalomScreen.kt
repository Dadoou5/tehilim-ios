package com.david.tehilim.features.shabbat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.tehilim.R
import com.david.tehilim.ui.theme.FrankRuhlLibreFontFamily
import com.david.tehilim.ui.theme.PinyonScriptFontFamily
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Écran affiché pendant Chabbat à la place du contenu de l'app (mirror iOS
 * ChabbatChalomView). « שבת שלום » + « Chabbat Chalom », heure de fin de
 * Chabbat selon la position, bougie animée, et bouton discret « Continuer
 * quand même ».
 */
@Composable
fun ChabbatChalomScreen(startsAt: Date?, endsAt: Date?, onContinue: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background
    val bgElevated = MaterialTheme.colorScheme.surface

    val visibleFlame = remember { Animatable(0f) }
    val visibleHebrew = remember { Animatable(0f) }
    val scaleHebrew = remember { Animatable(0.85f) }
    val visibleLatin = remember { Animatable(0f) }
    val visibleInfo = remember { Animatable(0f) }

    val flicker = rememberInfiniteTransition(label = "flicker")
    val flameScale by flicker.animateFloat(
        initialValue = 0.94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(450, easing = EaseInOut), RepeatMode.Reverse),
        label = "flameScale"
    )
    val glow by flicker.animateFloat(
        initialValue = 0.15f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { visibleFlame.animateTo(1f, tween(600, easing = EaseOut)) }
        }
        delay(300)
        coroutineScope {
            launch { visibleHebrew.animateTo(1f, tween(800, easing = EaseOut)) }
            launch { scaleHebrew.animateTo(1f, tween(800, easing = EaseOut)) }
        }
        delay(300)
        launch { visibleLatin.animateTo(1f, tween(800, easing = EaseOut)) }
        delay(400)
        launch { visibleInfo.animateTo(1f, tween(700, easing = EaseOut)) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(bg, bgElevated))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.LocalFireDepartment,
                contentDescription = null,
                tint = accent.copy(alpha = visibleFlame.value),
                modifier = Modifier.size(72.dp).scale(flameScale)
            )
            Spacer(Modifier.height(22.dp))

            Text(
                "שבת שלום",
                style = TextStyle(
                    fontFamily = FrankRuhlLibreFontFamily,
                    fontSize = 60.sp,
                    color = accent.copy(alpha = visibleHebrew.value),
                    textAlign = TextAlign.Center,
                    shadow = Shadow(color = accent.copy(alpha = glow * visibleHebrew.value), blurRadius = 24f)
                ),
                modifier = Modifier.scale(scaleHebrew.value)
            )
            Spacer(Modifier.height(12.dp))

            Text(
                "Chabbat Chalom",
                style = TextStyle(
                    fontFamily = PinyonScriptFontFamily,
                    fontSize = 44.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = visibleLatin.value),
                    textAlign = TextAlign.Center
                )
            )

            if (startsAt != null || endsAt != null) {
                Spacer(Modifier.height(24.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    if (startsAt != null) {
                        TimeRow(
                            title = stringResource(R.string.shabbat_starts_label),
                            value = endLabel(startsAt),
                            alpha = visibleInfo.value
                        )
                    }
                    if (endsAt != null) {
                        TimeRow(
                            title = stringResource(R.string.shabbat_ends_label),
                            value = endLabel(endsAt),
                            alpha = visibleInfo.value
                        )
                    }
                    Text(
                        stringResource(R.string.shabbat_end_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f * visibleInfo.value),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Échappatoire discrète en bas
        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 32.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextButton(onClick = onContinue) {
                Text(
                    stringResource(R.string.shabbat_continue_anyway),
                    style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = visibleInfo.value)
                )
            }
        }
    }
}

@Composable
private fun TimeRow(title: String, value: String, alpha: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            textAlign = TextAlign.Center
        )
    }
}

/** Ex. « samedi 31 mai · 22:14 ». */
private fun endLabel(date: Date): String {
    val day = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(date)
    val time = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(date)
    return "$day · $time"
}

package com.david.tehilim.features.splash

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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.david.tehilim.ui.theme.EzraSilFontFamily
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash écran de démarrage — mirror SplashView.swift V1.10.5.
 *
 * Apparitions animées séquentielles :
 *  - 0.00s : Hebrew « תהילים » (fade-in + scale-up 0.88→1.0, easeOut 0.8s)
 *  - 0.35s : Latin « Tehilim » (fade-in + translate-up 16dp)
 *  - 0.60s : glow oscille en permanence (easeInOut 1.6s)
 *  - 0.90s : dédicace hébreu (fade-in + slide-up)
 *  - 1.20s : dédicace français
 *
 * Total ~1.8s avant [onFinished], comme iOS.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val visibleHebrew = remember { Animatable(0f) }
    val scaleHebrew = remember { Animatable(0.88f) }
    val visibleLatin = remember { Animatable(0f) }
    val offsetLatin = remember { Animatable(16f) }
    val visibleDedicationHebrew = remember { Animatable(0f) }
    val offsetDedicationHebrew = remember { Animatable(8f) }
    val visibleDedicationLatin = remember { Animatable(0f) }
    val offsetDedicationLatin = remember { Animatable(8f) }

    val glowTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glowTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    LaunchedEffect(Unit) {
        // Hebrew title (alpha + scale en parallèle) — démarre immédiatement.
        coroutineScope {
            launch { visibleHebrew.animateTo(1f, tween(800, easing = EaseOut)) }
            launch { scaleHebrew.animateTo(1f, tween(800, easing = EaseOut)) }
        }
        // Latin (T + 0.35s)
        delay(350)
        coroutineScope {
            launch { visibleLatin.animateTo(1f, tween(800, easing = EaseOut)) }
            launch { offsetLatin.animateTo(0f, tween(800, easing = EaseOut)) }
        }
        // Dédicace hébreu (T + 0.9s — déjà 350ms écoulés)
        delay(550)
        coroutineScope {
            launch { visibleDedicationHebrew.animateTo(1f, tween(700, easing = EaseOut)) }
            launch { offsetDedicationHebrew.animateTo(0f, tween(700, easing = EaseOut)) }
        }
        // Dédicace latin (T + 1.2s)
        delay(300)
        coroutineScope {
            launch { visibleDedicationLatin.animateTo(1f, tween(700, easing = EaseOut)) }
            launch { offsetDedicationLatin.animateTo(0f, tween(700, easing = EaseOut)) }
        }
        // Pause contemplative puis on quitte (~1.8s total comme iOS).
        delay(600)
        onFinished()
    }

    val accent = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background
    val bgElevated = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(bg, bgElevated)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 40.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Spacer du haut pour reproduire la marge Spacer iOS
            Spacer(Modifier.height(80.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // תהילים — Hebrew RTL avec glow
                androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = "תהילים",
                        style = TextStyle(
                            fontFamily = EzraSilFontFamily,
                            fontSize = 88.sp,
                            color = accent.copy(alpha = visibleHebrew.value),
                            textAlign = TextAlign.Center,
                            shadow = Shadow(
                                color = accent.copy(alpha = glowAlpha * visibleHebrew.value),
                                blurRadius = 24f
                            )
                        ),
                        modifier = Modifier.scale(scaleHebrew.value)
                    )
                }

                // Tehilim — Latin serif italic
                Text(
                    text = "Tehilim",
                    style = TextStyle(
                        fontSize = 48.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        color = accent.copy(alpha = visibleLatin.value),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .padding(top = offsetLatin.value.dp.coerceAtLeast(0.dp))
                )
            }

            // Dédicace en bas
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 40.dp)
            ) {
                Box(
                    Modifier
                        .width(60.dp)
                        .height(0.5.dp)
                        .background(
                            MaterialTheme.colorScheme.outline.copy(
                                alpha = visibleDedicationHebrew.value
                            )
                        )
                )

                androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        "לעילוי נשמת ג׳והאן מאיר בן שרה בוגנים",
                        style = TextStyle(
                            fontFamily = EzraSilFontFamily,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                .copy(alpha = visibleDedicationHebrew.value)
                        ),
                        modifier = Modifier.padding(top = offsetDedicationHebrew.value.dp.coerceAtLeast(0.dp))
                    )
                }

                Text(
                    "Pour l'élévation de l'âme de Johann Meïr ben Sarah Bouganim",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                            .copy(alpha = 0.7f * visibleDedicationLatin.value)
                    ),
                    modifier = Modifier.padding(top = offsetDedicationLatin.value.dp.coerceAtLeast(0.dp))
                )
            }
        }
    }
}


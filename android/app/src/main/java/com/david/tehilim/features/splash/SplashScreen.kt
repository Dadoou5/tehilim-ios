package com.david.tehilim.features.splash

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.david.tehilim.R
import com.david.tehilim.ui.theme.FrankRuhlLibreFontFamily
import com.david.tehilim.ui.theme.PinyonScriptFontFamily
import kotlinx.coroutines.delay

/**
 * Splash écran de démarrage — mirror SplashView.swift V2.2.b.
 *
 * Animation « écriture » : les deux titres s'écrivent lettre à lettre,
 * simultanément — l'hébreu de droite à gauche (sens d'écriture), le latin
 * de gauche à droite. Chaque glyphe arrive comme un trait de plume : flou
 * d'encre, léger excès d'échelle, inclinaison calligraphique (latin), puis
 * se pose sur la ligne. Une fois les mots écrits, le halo pulse, le filet
 * se trace et les dédicaces montent.
 *
 * Chronologie (~2.6 s avant [onFinished], comme iOS) :
 *   0.00 → 0.95 s  תהילים s'écrit (6 lettres, cadence 120 ms)
 *   0.25 → 1.25 s  Tehilim s'écrit (révélation au masque, plume invisible)
 *   1.20 s         halo doré (pulse infini) + le filet se trace
 *   1.30 s         dédicace hébraïque
 *   1.55 s         dédicace latine
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        started = true
        delay(2600)
        onFinished()
    }

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
                // תהילים — la Row est verrouillée en RTL : l'index 0 (ת) se
                // place à droite et le délai croissant par index écrit donc
                // de droite à gauche.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row {
                        "תהילים".forEachIndexed { i, letter ->
                            val p by animateFloatAsState(
                                targetValue = if (started) 1f else 0f,
                                animationSpec = tween(550, delayMillis = i * 120, easing = EaseOut),
                                label = "heb$i"
                            )
                            Text(
                                text = letter.toString(),
                                style = TextStyle(
                                    fontFamily = FrankRuhlLibreFontFamily,
                                    fontSize = 96.sp,
                                    color = accent.copy(alpha = p),
                                    shadow = Shadow(
                                        color = accent.copy(alpha = glowAlpha * p),
                                        blurRadius = 24f
                                    )
                                ),
                                modifier = Modifier
                                    .graphicsLayer {
                                        val s = 1.3f - 0.3f * p
                                        scaleX = s
                                        scaleY = s
                                        translationY = 14.dp.toPx() * (1f - p)
                                        transformOrigin = TransformOrigin(0.5f, 1f)
                                    }
                                    .blur(((1f - p) * 8).dp, BlurredEdgeTreatment.Unbounded)
                            )
                        }
                    }
                }

                // Tehilim — calligraphie écrite de gauche à droite. Pinyon
                // est une cursive contextuelle : découpée lettre à lettre,
                // le shaping se dégrade (liaisons perdues). Le mot est donc
                // rendu ENTIER et révélé par un masque dégradé qui balaie de
                // gauche à droite — la « plume invisible » qui écrit. Bord
                // doux (36 dp) : l'encre semble apparaître, pas surgir.
                // Texte verrouillé en LTR même sous UI hébreu (RTL global).
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    val latinProgress by animateFloatAsState(
                        targetValue = if (started) 1f else 0f,
                        animationSpec = tween(1000, delayMillis = 250, easing = EaseInOut),
                        label = "latin"
                    )
                    Text(
                        text = "Tehilim",
                        style = TextStyle(
                            fontFamily = PinyonScriptFontFamily,
                            fontSize = 64.sp,
                            color = accent
                        ),
                        modifier = Modifier
                            .graphicsLayer {
                                // Couche offscreen requise pour que DstIn ne
                                // « troue » que le texte, pas le fond.
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                val pen = 36.dp.toPx()
                                val reveal = latinProgress * (size.width + pen)
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        0f to Color.Black,
                                        1f to Color.Transparent,
                                        startX = reveal - pen,
                                        endX = reveal
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    )
                }
            }

            // Dédicace en bas
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 40.dp)
            ) {
                // Le filet se « trace » (0 → 60 dp) une fois les mots écrits.
                val divider by animateFloatAsState(
                    targetValue = if (started) 1f else 0f,
                    animationSpec = tween(500, delayMillis = 1200, easing = EaseOut),
                    label = "divider"
                )
                Box(
                    Modifier
                        .width((60 * divider).dp)
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = divider))
                )

                val dedicationHebrew by animateFloatAsState(
                    targetValue = if (started) 1f else 0f,
                    animationSpec = tween(700, delayMillis = 1300, easing = EaseOut),
                    label = "dedicationHebrew"
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        "לעילוי נשמת ג׳והאן מאיר בן שרה בוגנים",
                        style = TextStyle(
                            fontFamily = FrankRuhlLibreFontFamily,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                .copy(alpha = dedicationHebrew)
                        ),
                        modifier = Modifier.graphicsLayer {
                            translationY = 8.dp.toPx() * (1f - dedicationHebrew)
                        }
                    )
                }

                val dedicationLatin by animateFloatAsState(
                    targetValue = if (started) 1f else 0f,
                    animationSpec = tween(700, delayMillis = 1550, easing = EaseOut),
                    label = "dedicationLatin"
                )
                Text(
                    stringResource(R.string.splash_dedication_french),
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                            .copy(alpha = 0.7f * dedicationLatin)
                    ),
                    modifier = Modifier.graphicsLayer {
                        translationY = 8.dp.toPx() * (1f - dedicationLatin)
                    }
                )
            }
        }
    }
}

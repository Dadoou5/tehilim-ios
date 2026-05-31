package com.david.tehilim.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.david.tehilim.MainActivity
import com.david.tehilim.R
import com.david.tehilim.TehilimApplication
import com.david.tehilim.core.model.DailyMode
import com.david.tehilim.core.model.Psalm
import com.david.tehilim.core.service.HebrewDateFormatter
import kotlinx.coroutines.flow.first

/**
 * Widget « Tehilim du jour » — mirror du DailyVerseWidget iOS V1.10.5.
 *
 * 3 tailles supportées (mirror iOS systemSmall / systemMedium / systemLarge) :
 * - **Small** (158×158) : header « Aujourd'hui » + grille 2 colonnes de
 *   chips (max 6) + footer compte total.
 * - **Medium** (338×158) : header « Tehilim du jour » + date hébraïque RTL
 *   en haut à droite + grille 4 colonnes (max 8 chips) + footer modeLabel
 *   + compte.
 * - **Large** (338×354) : header complet + date hébraïque + grille 3-4
 *   colonnes (max 12 chips) + verset accent (hébreu RTL + traduction FR).
 *
 * Glance n'a pas de LazyVGrid — on construit les grilles manuellement via
 * Row + Column. Le chip est un Box arrondi avec ID grand en bold +
 * hebrewNumber en petit en couleur accent.
 *
 * Tap n'importe où → ouvre l'app sur l'onglet Aujourd'hui via deep link
 * `tehilim://daily`.
 */
class DailyVerseWidget : GlanceAppWidget() {

    /** Tailles canoniques mirror iOS — Android demande de les déclarer ici. */
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(140.dp, 140.dp), // ~ systemSmall
            DpSize(280.dp, 140.dp), // ~ systemMedium
            DpSize(280.dp, 300.dp)  // ~ systemLarge
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TehilimApplication
        val container = app.container
        val mode = container.preferences.dailyMode.first()
        val psalmIds = container.dailyEngine.psalmsForToday(mode)
        // Pour les chips on a besoin du hebrewNumber → on map les IDs sur les
        // psaumes correspondants. Le repository est en mémoire, lookup O(n).
        val refs = psalmIds.mapNotNull { id -> container.psalmRepository.psalm(id) }
            .map { PsalmRef(it.id, it.hebrewNumber) }
        val firstVerse = refs.firstOrNull()?.let { container.psalmRepository.psalm(it.id)?.verses?.firstOrNull() }
        val firstVerseHebrew = firstVerse?.hebrew ?: ""
        val firstVerseFR = firstVerse?.translationFR
        val hebrewDate = HebrewDateFormatter.formatted().hebrew
        val modeLabel = context.getString(mode.labelRes)

        // Mode Chabbat : si actif et position connue, masque le contenu.
        val shabbatEnabled = container.preferences.shabbatEnabled.first()
        val lat = container.preferences.shabbatLatitude.first()
        val lon = container.preferences.shabbatLongitude.first()
        val shabbatState = if (shabbatEnabled && lat != null && lon != null) {
            com.david.tehilim.core.service.ShabbatCalculator.state(
                java.util.Date(),
                com.david.tehilim.core.service.GeoCoordinate(lat, lon)
            )
        } else {
            com.david.tehilim.core.service.ShabbatState(false, null, null)
        }

        provideContent {
            GlanceTheme {
                if (shabbatState.shouldDisplay) {
                    ShabbatWidgetContent(shabbatState.endsAt)
                } else {
                    WidgetContent(
                        refs = refs,
                        hebrewDate = hebrewDate,
                        modeLabel = modeLabel,
                        firstVerseHebrew = firstVerseHebrew,
                        firstVerseFR = firstVerseFR
                    )
                }
            }
        }
    }

    /** Mode Chabbat : « שבת שלום / Chabbat Chalom » + heure de fin. */
    @Composable
    private fun ShabbatWidgetContent(endsAt: java.util.Date?) {
        val context = LocalContext.current
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "שבת שלום",
                    style = TextStyle(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.primary,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    context.getString(R.string.shabbat_chalom_latin),
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                )
                if (endsAt != null) {
                    Spacer(GlanceModifier.height(6.dp))
                    val fmt = java.text.DateFormat.getTimeInstance(
                        java.text.DateFormat.SHORT, java.util.Locale.getDefault()
                    )
                    Text(
                        context.getString(R.string.shabbat_ends_short, fmt.format(endsAt)),
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = GlanceTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun WidgetContent(
        refs: List<PsalmRef>,
        hebrewDate: String,
        modeLabel: String,
        firstVerseHebrew: String,
        firstVerseFR: String?
    ) {
        val context = LocalContext.current
        val size = LocalSize.current

        val openDailyIntent = Intent(Intent.ACTION_VIEW, Uri.parse("tehilim://daily"))
            .setClass(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(12.dp)
                .clickable(actionStartActivity(openDailyIntent))
        ) {
            when {
                size.height >= 280.dp -> LargeLayout(refs, hebrewDate, modeLabel, firstVerseHebrew, firstVerseFR)
                size.width >= 260.dp -> MediumLayout(refs, hebrewDate, modeLabel)
                else -> SmallLayout(refs, modeLabel)
            }
        }
    }

    // MARK: - Small (~140×140)

    @Composable
    private fun SmallLayout(refs: List<PsalmRef>, modeLabel: String) {
        val context = LocalContext.current
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            HeaderLine(label = context.getString(R.string.label_today_short), compact = true)
            Spacer(GlanceModifier.height(6.dp))

            if (refs.isEmpty()) {
                Text(
                    "—",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            } else {
                val visible = refs.take(6)
                // Grille 2 colonnes
                visible.chunked(2).forEach { rowItems ->
                    Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp)) {
                        rowItems.forEachIndexed { idx, ref ->
                            if (idx > 0) Spacer(GlanceModifier.width(4.dp))
                            Box(modifier = GlanceModifier.defaultWeight()) {
                                PsalmChip(ref, compact = true)
                            }
                        }
                        // Cellule vide pour aligner si rangée impaire
                        if (rowItems.size == 1) {
                            Spacer(GlanceModifier.width(4.dp))
                            Spacer(GlanceModifier.defaultWeight())
                        }
                    }
                }
                if (refs.size > visible.size) {
                    Text(
                        context.getString(R.string.label_widget_more_n, refs.size - visible.size),
                        style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                context.getString(R.string.label_widget_psalm_count, refs.size),
                style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant)
            )
        }
    }

    // MARK: - Medium (~280×140)

    @Composable
    private fun MediumLayout(refs: List<PsalmRef>, hebrewDate: String, modeLabel: String) {
        val context = LocalContext.current
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header + date
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderLine(label = context.getString(R.string.section_today_psalms), compact = false)
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    hebrewDate,
                    style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant)
                )
            }
            Spacer(GlanceModifier.height(8.dp))

            // Grille 4 colonnes — max 8 chips
            if (refs.isEmpty()) {
                Text(
                    context.getString(R.string.msg_widget_no_psalm),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                )
            } else {
                val visible = refs.take(8)
                visible.chunked(4).forEach { rowItems ->
                    Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)) {
                        rowItems.forEachIndexed { idx, ref ->
                            if (idx > 0) Spacer(GlanceModifier.width(6.dp))
                            Box(modifier = GlanceModifier.defaultWeight()) {
                                PsalmChip(ref, compact = false)
                            }
                        }
                        // remplit la rangée pour conserver la largeur du chip
                        val padding = 4 - rowItems.size
                        if (padding > 0) repeat(padding) {
                            Spacer(GlanceModifier.width(6.dp))
                            Spacer(GlanceModifier.defaultWeight())
                        }
                    }
                }
                if (refs.size > visible.size) {
                    Text(
                        context.getString(R.string.label_widget_more_psalms_n, refs.size - visible.size),
                        style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Footer mode + count
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modeLabel,
                    style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant)
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    context.getString(R.string.label_widget_psalm_count, refs.size),
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.primary
                    )
                )
            }
        }
    }

    // MARK: - Large (~280×300)

    @Composable
    private fun LargeLayout(
        refs: List<PsalmRef>,
        hebrewDate: String,
        modeLabel: String,
        firstVerseHebrew: String,
        firstVerseFR: String?
    ) {
        val context = LocalContext.current
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HeaderLine(label = context.getString(R.string.section_today_psalms), compact = false)
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    modeLabel,
                    style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant)
                )
            }
            Text(
                hebrewDate,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    color = GlanceTheme.colors.onSurface
                ),
                modifier = GlanceModifier.fillMaxWidth()
            )
            Spacer(GlanceModifier.height(8.dp))

            // Grille 3-4 colonnes — max 12 chips
            if (refs.isNotEmpty()) {
                val cols = if (refs.size > 8) 4 else 3
                val visible = refs.take(12)
                visible.chunked(cols).forEach { rowItems ->
                    Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)) {
                        rowItems.forEachIndexed { idx, ref ->
                            if (idx > 0) Spacer(GlanceModifier.width(6.dp))
                            Box(modifier = GlanceModifier.defaultWeight()) {
                                PsalmChip(ref, compact = false)
                            }
                        }
                        val padding = cols - rowItems.size
                        if (padding > 0) repeat(padding) {
                            Spacer(GlanceModifier.width(6.dp))
                            Spacer(GlanceModifier.defaultWeight())
                        }
                    }
                }
                if (refs.size > visible.size) {
                    Text(
                        context.getString(R.string.label_widget_more_psalms_n, refs.size - visible.size),
                        style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant),
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Verset accent (hébreu + traduction FR si disponible)
            if (firstVerseHebrew.isNotEmpty()) {
                Text(
                    firstVerseHebrew,
                    style = TextStyle(
                        fontSize = 13.sp,
                        textAlign = TextAlign.End,
                        color = GlanceTheme.colors.onSurface
                    ),
                    maxLines = 2,
                    modifier = GlanceModifier.fillMaxWidth()
                )
                if (!firstVerseFR.isNullOrEmpty()) {
                    Text(
                        firstVerseFR,
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        ),
                        maxLines = 2
                    )
                }
            }
        }
    }

    // MARK: - Chip + Header helpers

    @Composable
    private fun PsalmChip(ref: PsalmRef, compact: Boolean) {
        // V1.4 — hauteur passée de fixe (28/38 dp) à `padding vertical`
        // intrinsèque, sinon le hebrewNumber sur la 2ᵉ ligne était tronqué
        // verticalement. Police hébreu également bumpée (9/10 → 11/13 sp)
        // car les caractères hébraïques (ex. « כ״ג ») ont un trait fin qui
        // devient illisible sous 10sp dans Glance.
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(day = Color(0xFFE6EEF6), night = Color(0xFF1E2A38)))
                .cornerRadius(8.dp)
                .padding(horizontal = 4.dp, vertical = if (compact) 4.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                ref.id.toString(),
                style = TextStyle(
                    fontSize = if (compact) 13.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface
                )
            )
            Text(
                ref.hebrewNumber,
                style = TextStyle(
                    fontSize = if (compact) 11.sp else 13.sp,
                    color = GlanceTheme.colors.primary
                ),
                maxLines = 1
            )
        }
    }

    @Composable
    private fun HeaderLine(label: String, compact: Boolean) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "☀",
                style = TextStyle(
                    fontSize = if (compact) 11.sp else 13.sp,
                    color = GlanceTheme.colors.primary
                )
            )
            Spacer(GlanceModifier.width(4.dp))
            Text(
                label,
                style = TextStyle(
                    fontSize = if (compact) 11.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        }
    }

    private data class PsalmRef(val id: Int, val hebrewNumber: String)
}

class DailyVerseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyVerseWidget()
}

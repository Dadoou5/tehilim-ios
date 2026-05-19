package com.david.tehilim.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.david.tehilim.MainActivity
import com.david.tehilim.R
import com.david.tehilim.TehilimApplication
import com.david.tehilim.core.service.HebrewDateFormatter
import kotlinx.coroutines.flow.first

/**
 * Widget « Tehilim du jour » — équivalent du WidgetKit iOS DailyVerseWidget.
 * Affiche la liste des Tehilim du jour + la date hébraïque compacte.
 * Tap → ouvre l'app (onglet Aujourd'hui via deep link tehilim://daily).
 */
class DailyVerseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TehilimApplication
        val container = app.container
        val mode = container.preferences.dailyMode.first()
        val psalmIds = container.dailyEngine.psalmsForToday(mode)
        val hebrewDate = HebrewDateFormatter.compact()

        provideContent {
            WidgetContent(psalmIds = psalmIds, hebrewDate = hebrewDate)
        }
    }

    @Composable
    private fun WidgetContent(psalmIds: List<Int>, hebrewDate: String) {
        // Deep link tehilim://daily — ouvre l'app directement sur l'onglet
        // « Aujourd'hui ». Mirror du widget iOS V1.10.5.
        val context = LocalContext.current
        val openDailyIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("tehilim://daily")
        ).setClass(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFFF4F8FB))
                .padding(12.dp)
                .clickable(actionStartActivity(openDailyIntent)),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                context.getString(R.string.section_today_psalms),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ColorProvider(Color(0xFF1E6091))
                )
            )
            if (psalmIds.isEmpty()) {
                Text(
                    context.getString(R.string.msg_widget_no_psalm),
                    style = TextStyle(color = ColorProvider(Color(0xFF5C6B7A)))
                )
            } else {
                Text(
                    psalmIds.take(8).joinToString(" · "),
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = ColorProvider(Color(0xFF0F1F2E))
                    )
                )
                if (psalmIds.size > 8) {
                    Text(
                        "+${psalmIds.size - 8}",
                        style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color(0xFF8A99A7)))
                    )
                }
            }
            Text(
                hebrewDate,
                style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color(0xFF8A99A7)))
            )
        }
    }
}

class DailyVerseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyVerseWidget()
}

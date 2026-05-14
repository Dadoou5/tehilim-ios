package com.david.tehilim.features.sharing

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.david.tehilim.core.model.Psalm
import com.david.tehilim.core.model.TranslationLanguage
import com.david.tehilim.core.model.Verse
import java.io.File
import java.io.FileOutputStream

/**
 * Génère une image 1080×1080 stylisée d'un verset pour partage.
 * Mirror simplifié du VerseShareCard SwiftUI.
 *
 * Le rendu se fait via Canvas Android (pas Compose) pour rester léger
 * et n'allouer la bitmap qu'au moment du share (cf. fix iOS V1.10.4).
 */
object VerseShareRenderer {

    private const val SIZE = 1080

    /**
     * Rend, écrit dans un fichier temp + retourne l'Uri partageable.
     * À appeler dans une coroutine (I/O).
     */
    fun renderAndShare(
        context: Context,
        psalm: Psalm,
        verse: Verse,
        translationLang: TranslationLanguage
    ) {
        val bitmap = renderBitmap(context, psalm, verse, translationLang)
        val uri = saveAsTempFile(context, bitmap, psalm.id, verse.number)
        shareImage(context, uri, psalm.id, verse.number)
    }

    private fun renderBitmap(
        context: Context,
        psalm: Psalm,
        verse: Verse,
        translationLang: TranslationLanguage
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fond bleu eau
        canvas.drawColor(Color.parseColor("#F4F8FB"))

        // Titre — accent
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E6091")
            textSize = 56f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("Tehilim ${psalm.id} · ${psalm.hebrewNumber}", SIZE / 2f, 140f, titlePaint)

        // Hébreu — large, centré
        val hebrewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F1F2E")
            textSize = 64f
            textAlign = Paint.Align.CENTER
        }
        // Charge la police Ezra SIL SR si dispo
        runCatching {
            hebrewPaint.typeface = Typeface.createFromAsset(context.assets, "data/dummy.ttf")
        } // fallback : système si pas dispo

        drawWrappedText(canvas, verse.hebrew, hebrewPaint, RectF(80f, 250f, SIZE - 80f, 600f))

        // Traduction — italique secondaire
        val translation = verse.translation(translationLang)
        if (!translation.isNullOrBlank()) {
            val trPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#5C6B7A")
                textSize = 42f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            }
            drawWrappedText(canvas, translation, trPaint, RectF(80f, 650f, SIZE - 80f, 900f))
        }

        // Footer : verset + source
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8A99A7")
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Verset ${verse.number}", SIZE / 2f, 970f, footerPaint)
        canvas.drawText(translationLang.sourceCredit, SIZE / 2f, 1020f, footerPaint)

        return bitmap
    }

    private fun drawWrappedText(canvas: Canvas, text: String, paint: Paint, bounds: RectF) {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        for (w in words) {
            val candidate = if (current.isEmpty()) w else "$current $w"
            if (paint.measureText(candidate) > bounds.width()) {
                if (current.isNotEmpty()) lines += current
                current = w
            } else {
                current = candidate
            }
        }
        if (current.isNotEmpty()) lines += current

        val lineHeight = paint.textSize * 1.3f
        val totalHeight = lineHeight * lines.size
        val startY = bounds.centerY() - totalHeight / 2f + paint.textSize
        for ((i, line) in lines.withIndex()) {
            canvas.drawText(line, bounds.centerX(), startY + i * lineHeight, paint)
        }
    }

    private fun saveAsTempFile(context: Context, bitmap: Bitmap, psalmId: Int, verseNumber: Int): Uri {
        val cacheDir = File(context.cacheDir, "shared_verses").apply { mkdirs() }
        val file = File(cacheDir, "Tehilim_${psalmId}_v${verseNumber}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun shareImage(context: Context, uri: Uri, psalmId: Int, verseNumber: Int) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Tehilim $psalmId · verset $verseNumber")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Partager le verset").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

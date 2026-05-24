package com.david.tehilim.core.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.david.tehilim.R
import com.david.tehilim.core.model.SavedPrayerIntent
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Planifie un rappel quotidien à l'heure choisie via WorkManager.
 * Mirror du NotificationManager iOS (UNUserNotificationCenter).
 *
 * Limitations Android :
 * - WorkManager garantit l'exécution mais pas à la milliseconde près
 * - Pour une notification exactement à l'heure, AlarmManager.setExactAndAllowWhileIdle
 *   serait plus précis mais consomme la batterie. WorkManager suffit pour un rappel.
 */
object NotificationScheduler {

    const val DAILY_WORK_NAME = "tehilim.daily.reminder"
    const val CHANNEL_ID = "tehilim.daily"

    /**
     * Schedule un rappel quotidien à `hour:minute` (heure locale).
     * Si un rappel existe déjà, il est remplacé (REPLACE policy).
     */
    fun scheduleDaily(context: Context, hour: Int, minute: Int) {
        ensureChannel(context)

        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        val initialDelay = Duration.between(now, target).toMillis()

        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().build())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelDaily(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(DAILY_WORK_NAME)
    }

    // ─── V1.4 — Commémoration (azcara) ───────────────────────────────────

    /** Préfixes UniqueWork pour pouvoir annuler ciblé un intent. */
    fun memorialWorkName(intentId: String, kind: String) =
        "tehilim.memorial.$intentId.$kind"

    /**
     * Annule les rappels d'azcara pour un intent (J-7 et jour J).
     * Appelé quand l'utilisateur désactive les rappels, modifie la date,
     * ou supprime le Lelouy Nichmat.
     */
    fun cancelMemorial(context: Context, intentId: String) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(memorialWorkName(intentId, "j7"))
        wm.cancelUniqueWork(memorialWorkName(intentId, "day"))
    }

    /**
     * Reprogramme (annule + re-planifie) les rappels d'azcara pour un intent.
     * No-op si les conditions ne sont pas réunies (rappels off, pas de date,
     * aucun toggle activé, ou prochaine azcara dans le passé).
     */
    fun rescheduleMemorial(context: Context, intent: SavedPrayerIntent) {
        cancelMemorial(context, intent.id)

        val deathMillis = intent.civilDateOfDeathEpochMillis ?: return
        if (!intent.remindersEnabled) return
        if (!intent.notifySevenDaysBefore && !intent.notifySameDay) return

        val deathDate = Date(deathMillis)
        val nextAzcara = MemorialCalculator.nextYahrzeit(deathDate) ?: return
        ensureChannel(context)

        val wm = WorkManager.getInstance(context)
        val now = System.currentTimeMillis()
        val nextMillis = nextAzcara.time
        val subject = intent.hebrewSubject

        if (intent.notifySevenDaysBefore) {
            val triggerMillis = nextMillis - 7L * 24 * 60 * 60 * 1000
            if (triggerMillis > now) {
                val req = OneTimeWorkRequestBuilder<MemorialReminderWorker>()
                    .setInitialDelay(triggerMillis - now, TimeUnit.MILLISECONDS)
                    .setInputData(
                        Data.Builder()
                            .putString(MemorialReminderWorker.KEY_INTENT_ID, intent.id)
                            .putString(MemorialReminderWorker.KEY_SUBJECT, subject)
                            .putInt(MemorialReminderWorker.KEY_DAYS_OFFSET, 7)
                            .build()
                    )
                    .build()
                wm.enqueueUniqueWork(
                    memorialWorkName(intent.id, "j7"),
                    ExistingWorkPolicy.REPLACE,
                    req
                )
            }
        }

        if (intent.notifySameDay) {
            // Décaler à 9h locale pour ne pas réveiller la nuit.
            val cal = java.util.Calendar.getInstance().apply { time = nextAzcara }
            cal.set(java.util.Calendar.HOUR_OF_DAY, 9)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            val sameDayMillis = cal.timeInMillis
            if (sameDayMillis > now) {
                val req = OneTimeWorkRequestBuilder<MemorialReminderWorker>()
                    .setInitialDelay(sameDayMillis - now, TimeUnit.MILLISECONDS)
                    .setInputData(
                        Data.Builder()
                            .putString(MemorialReminderWorker.KEY_INTENT_ID, intent.id)
                            .putString(MemorialReminderWorker.KEY_SUBJECT, subject)
                            .putInt(MemorialReminderWorker.KEY_DAYS_OFFSET, 0)
                            .build()
                    )
                    .build()
                wm.enqueueUniqueWork(
                    memorialWorkName(intent.id, "day"),
                    ExistingWorkPolicy.REPLACE,
                    req
                )
            }
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_description)
            }
            nm.createNotificationChannel(channel)
        }
    }
}

/**
 * Worker exécuté par WorkManager — affiche la notification.
 */
class DailyReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        NotificationScheduler.ensureChannel(context)

        // Vérifie la permission POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success() // pas de permission, on skip silencieusement
        }

        val deepLink = Uri.parse("tehilim://daily")
        val intent = Intent(Intent.ACTION_VIEW, deepLink).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            setPackage(context.packageName)
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(context.getString(R.string.notif_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1, notif)
        return Result.success()
    }
}

/**
 * V1.4 — Worker dédié aux rappels d'azcara (J-7 + jour J).
 * Identique au DailyReminderWorker mais avec un payload paramétré
 * (subject hébreu + nombre de jours avant l'azcara) pour personnaliser
 * le titre et le corps.
 */
class MemorialReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_INTENT_ID = "intentId"
        const val KEY_SUBJECT = "subject"
        const val KEY_DAYS_OFFSET = "daysOffset"
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        NotificationScheduler.ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        val subject = inputData.getString(KEY_SUBJECT) ?: ""
        val daysOffset = inputData.getInt(KEY_DAYS_OFFSET, 0)
        val intentId = inputData.getString(KEY_INTENT_ID) ?: "memorial"

        val title = context.getString(
            if (daysOffset == 0) R.string.memorial_notif_title_today
            else R.string.memorial_notif_title_j7
        )
        val body = if (daysOffset == 0)
            context.getString(R.string.memorial_notif_body_today, subject)
        else
            context.getString(R.string.memorial_notif_body_j7, subject, daysOffset)

        // Tap → ouvre l'app sur la liste des prières sauvegardées.
        val deepLink = Uri.parse("tehilim://saved-prayers")
        val intent = Intent(Intent.ACTION_VIEW, deepLink).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            setPackage(context.packageName)
        }
        val pending = PendingIntent.getActivity(
            context, intentId.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        // ID unique par intent pour ne pas écraser une autre notif azcara
        // (cas où l'utilisateur a plusieurs Lelouy Nichmat).
        NotificationManagerCompat.from(context).notify(intentId.hashCode(), notif)
        return Result.success()
    }
}

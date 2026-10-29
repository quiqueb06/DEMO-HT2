package com.example.hojadetrabajo2demodepermisosycrashlytics.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Manages the creation and display of alarm notifications.
 * Encapsulates all Android notification API calls so that neither the
 * ViewModel nor the UI layer depend on the notification SDK directly.
 *
 * @param context Application context — safe to store long-term.
 */
class AlarmNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID   = "sheep_alarm_channel"
        const val CHANNEL_NAME = "Sheep Alarm"
        const val NOTIFICATION_ID = 1001
    }

    /**
     * Creates the notification channel required on Android 8+ (API 26).
     * Safe to call multiple times; the system ignores duplicates.
     * Call this once from [MainActivity.onCreate].
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when the sheep alarm time is reached"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts the alarm-fired notification.
     * On Android 13+ this call requires POST_NOTIFICATIONS; the caller
     * (ViewModel) is responsible for verifying the permission before invoking.
     * A SecurityException is caught and returned as a [Result] failure so the
     * ViewModel can forward it to [CrashReporter].
     */
    fun showAlarmNotification(): Result<Unit> = runCatching {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🐑 ¡Sheep Alarm!")
            .setContentText("Wake up! The sheep have stopped jumping. It's time!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /** Cancels the alarm notification if it is still visible in the tray. */
    fun cancelNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}

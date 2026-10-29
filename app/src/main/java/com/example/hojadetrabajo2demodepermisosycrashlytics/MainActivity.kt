package com.example.hojadetrabajo2demodepermisosycrashlytics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.hojadetrabajo2demodepermisosycrashlytics.notification.AlarmNotificationManager
import com.example.hojadetrabajo2demodepermisosycrashlytics.ui.screen.AlarmScreen
import com.example.hojadetrabajo2demodepermisosycrashlytics.ui.theme.HojaDeTrabajo2DemoDePermisosYCrashlyticsTheme

/**
 * Entry-point Activity.
 * Responsibilities are intentionally minimal:
 *   1. Create the notification channel (must happen before any notification can be shown).
 *   2. Host [AlarmScreen] inside the app theme.
 *
 * No business logic, no ViewModel access, no data-layer imports live here.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channel once on startup.
        // Safe to call multiple times — the OS ignores duplicate channel creation.
        AlarmNotificationManager(applicationContext).createNotificationChannel()

        setContent {
            HojaDeTrabajo2DemoDePermisosYCrashlyticsTheme {
                AlarmScreen()
            }
        }
    }
}
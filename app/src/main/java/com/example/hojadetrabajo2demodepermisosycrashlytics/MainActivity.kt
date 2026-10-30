package com.example.hojadetrabajo2demodepermisosycrashlytics

import android.os.Bundle
import com.example.hojadetrabajo2demodepermisosycrashlytics.data.datasource.AndroidPermissionDataSource
import com.example.hojadetrabajo2demodepermisosycrashlytics.data.repository.PermissionRepositoryImpl
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hojadetrabajo2demodepermisosycrashlytics.data.datasource.AlarmLocalDataSource
import com.example.hojadetrabajo2demodepermisosycrashlytics.data.repository.AlarmRepositoryImpl
import com.example.hojadetrabajo2demodepermisosycrashlytics.domain.crash.LogCrashReporter
import com.example.hojadetrabajo2demodepermisosycrashlytics.viewmodel.AlarmViewModel
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
 * Wires concrete dependencies at the composition root; business logic stays in the ViewModel.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channel once on startup.
        // Safe to call multiple times — the OS ignores duplicate channel creation.
        val reporter = LogCrashReporter()
        val permissions = PermissionRepositoryImpl(AndroidPermissionDataSource(applicationContext))
        val notifications = AlarmNotificationManager(applicationContext, permissions)
        runCatching { notifications.createNotificationChannel() }.onFailure(reporter::report)
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(AlarmViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return AlarmViewModel(AlarmRepositoryImpl(AlarmLocalDataSource()), notifications, reporter, permissions) as T
            }
        }

        setContent {
            HojaDeTrabajo2DemoDePermisosYCrashlyticsTheme {
                AlarmScreen(viewModel = viewModel(factory = factory))
            }
        }
    }
}
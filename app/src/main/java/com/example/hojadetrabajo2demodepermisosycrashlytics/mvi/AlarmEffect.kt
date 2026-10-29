package com.example.hojadetrabajo2demodepermisosycrashlytics.mvi

/**
 * One-shot side-effects emitted by the ViewModel via a [kotlinx.coroutines.channels.Channel].
 * Unlike [AlarmUiState], effects are consumed exactly once and never replayed.
 *
 * The UI layer subscribes via [kotlinx.coroutines.flow.Flow.collect] inside a [LaunchedEffect].
 */
sealed class AlarmEffect {

    /** Show the system notification that the alarm fired. */
    object TriggerAlarmNotification : AlarmEffect()

    /** Cancel the alarm notification (e.g., user dismissed it in-app). */
    object CancelAlarmNotification : AlarmEffect()

    /**
     * Signal the [PermissionHandler] composable to launch the OS permission dialog
     * for POST_NOTIFICATIONS.
     */
    object LaunchNotificationPermission : AlarmEffect()

    /** Open the app's system Settings page so the user can re-enable notifications. */
    object OpenAppSettings : AlarmEffect()
}

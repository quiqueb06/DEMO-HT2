package com.example.hojadetrabajo2demodepermisosycrashlytics.mvi

/**
 * Sealed hierarchy of all user actions (intents) the ViewModel can process.
 * MVI rule: the UI layer sends ONLY [AlarmIntent] objects — never calls
 * repository or data-source methods directly.
 */
sealed class AlarmIntent {

    /** User confirmed a new alarm time in the picker. */
    data class SetAlarm(val hour: Int, val minute: Int) : AlarmIntent()

    /** User pressed "Cancel Alarm" button. */
    object CancelAlarm : AlarmIntent()

    /** User dismissed the triggered-alarm overlay. */
    object DismissAlarm : AlarmIntent()

    /** User pressed "Set Alarm" / "Change Alarm" button. */
    object ShowTimePicker : AlarmIntent()

    /** User dismissed the time picker without selecting a time. */
    object HideTimePicker : AlarmIntent()

    /**
     * Delivered by [PermissionHandler] after the system permission dialog closes.
     *
     * @param granted          True if the user granted POST_NOTIFICATIONS.
     * @param permanentlyDenied True when "Don't ask again" was checked.
     */
    data class NotificationPermissionResult(
        val granted: Boolean,
        val permanentlyDenied: Boolean
    ) : AlarmIntent()

    /** User pressed "Enable Notifications" in the rationale dialog. */
    object RequestNotificationPermission : AlarmIntent()

    data class SetNotificationsEnabled(val enabled: Boolean) : AlarmIntent()

    /** Refresh from the platform on resume, without opening a rationale dialog. */
    data class SyncNotificationPermission(val granted: Boolean, val permanentlyDenied: Boolean) : AlarmIntent()

    data class ReportError(val error: Throwable) : AlarmIntent()

    /** User pressed "Open Settings" from the permanently-denied rationale. */
    object OpenSettings : AlarmIntent()

    /** User dismissed the notification rationale dialog. */
    object DismissNotificationRationale : AlarmIntent()
}

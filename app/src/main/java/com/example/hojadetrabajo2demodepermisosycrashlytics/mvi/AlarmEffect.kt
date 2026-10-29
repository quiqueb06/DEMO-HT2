package com.example.hojadetrabajo2demodepermisosycrashlytics.mvi

/** UI-only effects. Notification delivery belongs to the injected notifier. */
sealed class AlarmEffect {
    object LaunchNotificationPermission : AlarmEffect()
    object OpenAppSettings : AlarmEffect()
}

package com.example.hojadetrabajo2demodepermisosycrashlytics.notification

/** Platform boundary, injected into the ViewModel and replaced by a fake in tests. */
interface AlarmNotifier {
    var notificationsEnabled: Boolean
    fun canNotify(): Boolean
    fun showAlarmNotification(): Result<Unit>
    fun cancelNotification()
}

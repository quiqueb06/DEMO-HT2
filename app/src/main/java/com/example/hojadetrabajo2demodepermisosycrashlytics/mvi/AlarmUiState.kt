package com.example.hojadetrabajo2demodepermisosycrashlytics.mvi

import com.example.hojadetrabajo2demodepermisosycrashlytics.domain.permission.NotificationPermissionStatus

/**
 * Single, immutable snapshot of everything the UI needs to render.
 * MVI rule: the UI layer reads ONLY from this state object.
 *
 * @property currentTime              Formatted "HH:mm:ss" string updated every second.
 * @property alarmHour                Hour the alarm is set to (0-23).
 * @property alarmMinute              Minute the alarm is set to (0-59).
 * @property isAlarmSet               Whether an alarm is actively counting down.
 * @property isAlarmTriggered         Whether the alarm time has been reached.
 * @property isSheepJumping           Controls the jumping loop in [SheepJumpingAnimation].
 * @property notificationPermission   Current POST_NOTIFICATIONS permission state.
 * @property showTimePicker           Whether to display the time-picker dialog.
 * @property showNotificationRationale Whether to display the rationale dialog.
 */
data class AlarmUiState(
    val notificationRuntimePermission: String? = null,
    val notificationsEnabled: Boolean = false,
    val errorMessage: String? = null,
    val currentTime: String = "00:00:00",
    val alarmHour: Int = 0,
    val alarmMinute: Int = 0,
    val isAlarmSet: Boolean = false,
    val isAlarmTriggered: Boolean = false,
    val isSheepJumping: Boolean = false,
    val notificationPermission: NotificationPermissionStatus = NotificationPermissionStatus.NOT_REQUESTED,
    val showTimePicker: Boolean = false,
    val showNotificationRationale: Boolean = false
)

package com.example.hojadetrabajo2demodepermisosycrashlytics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hojadetrabajo2demodepermisosycrashlytics.data.repository.AlarmRepository
import com.example.hojadetrabajo2demodepermisosycrashlytics.domain.crash.CrashReporter
import com.example.hojadetrabajo2demodepermisosycrashlytics.mvi.*
import com.example.hojadetrabajo2demodepermisosycrashlytics.notification.AlarmNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** State and business logic only. Android dependencies are supplied at the app entry point. */
class AlarmViewModel(
    private val repository: AlarmRepository,
    private val notifier: AlarmNotifier,
    private val crashReporter: CrashReporter
) : ViewModel() {
    private val _uiState = MutableStateFlow(AlarmUiState(notificationsEnabled = notifier.notificationsEnabled))
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()
    private val _effects = Channel<AlarmEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                safely { repository.tick() }
                delay(1_000)
            }
        }
        viewModelScope.launch {
            combine(repository.currentTime, repository.alarm, repository.alarmTriggered) { time, alarm, triggered ->
                Triple(time, alarm, triggered)
            }.collect { (time, alarm, triggered) ->
                val justTriggered = triggered && !_uiState.value.isAlarmTriggered
                _uiState.update {
                    it.copy(currentTime = time, alarmHour = alarm.hour, alarmMinute = alarm.minute,
                        isAlarmSet = alarm.isActive, isAlarmTriggered = triggered,
                        isSheepJumping = alarm.isActive && !triggered)
                }
                if (justTriggered) safely {
                    crashReporter.log("Alarm reached; sheep stopped")
                    if (_uiState.value.notificationsEnabled && notifier.canNotify()) {
                        notifier.showAlarmNotification().getOrThrow()
                    }
                }
            }
        }
    }

    fun processIntent(intent: AlarmIntent) {
        viewModelScope.launch {
            safely {
                when (intent) {
                    is AlarmIntent.SetAlarm -> {
                        repository.setAlarm(intent.hour, intent.minute)
                        notifier.cancelNotification()
                        _uiState.update { it.copy(showTimePicker = false, errorMessage = null) }
                    }
                    AlarmIntent.CancelAlarm -> { repository.cancelAlarm(); notifier.cancelNotification() }
                    AlarmIntent.DismissAlarm -> { repository.dismissAlarm(); notifier.cancelNotification() }
                    AlarmIntent.ShowTimePicker -> _uiState.update { it.copy(showTimePicker = true) }
                    AlarmIntent.HideTimePicker -> _uiState.update { it.copy(showTimePicker = false) }
                    AlarmIntent.RequestNotificationPermission -> enableNotifications()
                    is AlarmIntent.SetNotificationsEnabled -> {
                        notifier.notificationsEnabled = intent.enabled
                        _uiState.update { it.copy(notificationsEnabled = intent.enabled) }
                        if (intent.enabled) enableNotifications() else notifier.cancelNotification()
                    }
                    is AlarmIntent.NotificationPermissionResult -> {
                        updatePermission(intent.granted, intent.permanentlyDenied)
                        _uiState.update { it.copy(showNotificationRationale = !intent.granted) }
                    }
                    is AlarmIntent.SyncNotificationPermission -> updatePermission(intent.granted, intent.permanentlyDenied)
                    is AlarmIntent.ReportError -> throw intent.error
                    AlarmIntent.OpenSettings -> {
                        _uiState.update { it.copy(showNotificationRationale = false) }
                        _effects.send(AlarmEffect.OpenAppSettings)
                    }
                    AlarmIntent.DismissNotificationRationale -> _uiState.update { it.copy(showNotificationRationale = false) }
                }
            }
        }
    }

    private suspend fun enableNotifications() {
        notifier.notificationsEnabled = true
        _uiState.update { it.copy(notificationsEnabled = true, showNotificationRationale = false) }
        when {
            notifier.canNotify() -> updatePermission(true, false)
            _uiState.value.notificationPermission == NotificationPermissionStatus.PERMANENTLY_DENIED ->
                _uiState.update { it.copy(showNotificationRationale = true) }
            else -> _effects.send(AlarmEffect.LaunchNotificationPermission)
        }
    }

    private fun updatePermission(granted: Boolean, permanent: Boolean) {
        _uiState.update { it.copy(
            notificationPermission = when {
                granted && notifier.canNotify() -> NotificationPermissionStatus.GRANTED
                permanent || granted -> NotificationPermissionStatus.PERMANENTLY_DENIED
                else -> NotificationPermissionStatus.DENIED
            },
            showNotificationRationale = if (granted && notifier.canNotify()) false else it.showNotificationRationale
        ) }
    }

    private inline fun safely(block: () -> Unit) {
        try { block() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            crashReporter.report(error)
            _uiState.update { it.copy(errorMessage = "No se pudo completar la operación. Intenta nuevamente.") }
        }
    }
}

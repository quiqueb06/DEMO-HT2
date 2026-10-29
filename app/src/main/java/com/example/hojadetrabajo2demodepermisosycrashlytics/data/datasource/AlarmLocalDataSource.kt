package com.example.hojadetrabajo2demodepermisosycrashlytics.data.datasource

import com.example.hojadetrabajo2demodepermisosycrashlytics.data.model.AlarmModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * Local data source responsible for holding and exposing alarm state and the current time.
 * All mutable state is kept private; only read-only flows are exposed.
 */
class AlarmLocalDataSource {

    private val _alarm = MutableStateFlow(AlarmModel())
    val alarm: StateFlow<AlarmModel> = _alarm.asStateFlow()

    private val _currentTime = MutableStateFlow(getCurrentTimeString())
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _alarmTriggered = MutableStateFlow(false)
    val alarmTriggered: StateFlow<Boolean> = _alarmTriggered.asStateFlow()

    /** Saves a new alarm configuration. */
    fun saveAlarm(alarm: AlarmModel) {
        _alarm.value = alarm
        _alarmTriggered.value = false
    }

    /** Cancels the current alarm. */
    fun cancelAlarm() {
        _alarm.value = AlarmModel()
        _alarmTriggered.value = false
    }

    /** Updates the current time string and checks whether the alarm should trigger. */
    fun refreshCurrentTime() {
        val now = getCurrentTimeString()
        _currentTime.value = now

        val alarmState = _alarm.value
        if (alarmState.isActive && !_alarmTriggered.value) {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            if (currentHour == alarmState.hour && currentMinute == alarmState.minute) {
                _alarmTriggered.value = true
                _alarm.value = alarmState.copy(isActive = false)
            }
        }
    }

    /** Acknowledges an alarm trigger and resets state. */
    fun acknowledgeAlarm() {
        _alarmTriggered.value = false
        _alarm.value = AlarmModel()
    }

    private fun getCurrentTimeString(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val minute = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
        val second = calendar.get(Calendar.SECOND).toString().padStart(2, '0')
        return "$hour:$minute:$second"
    }
}

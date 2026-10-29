package com.example.hojadetrabajo2demodepermisosycrashlytics.data.repository

import com.example.hojadetrabajo2demodepermisosycrashlytics.data.model.AlarmModel
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for the alarm data repository.
 * The ViewModel depends on this interface, never on the concrete implementation,
 * which makes the ViewModel independently testable.
 */
interface AlarmRepository {

    /** Read-only stream of the active alarm configuration. */
    val alarm: StateFlow<AlarmModel>

    /** Read-only stream of the current time formatted as "HH:mm:ss". */
    val currentTime: StateFlow<String>

    /**
     * Emits `true` exactly once when the current time matches the alarm time.
     * Resets to `false` when [dismissAlarm] is called.
     */
    val alarmTriggered: StateFlow<Boolean>

    /**
     * Persists and activates a new alarm.
     * @param hour   0–23
     * @param minute 0–59
     */
    fun setAlarm(hour: Int, minute: Int)

    /** Deactivates the alarm without triggering it. */
    fun cancelAlarm()

    /**
     * Advances the internal clock by one tick and evaluates whether the alarm
     * should fire. Must be called once per second by the ViewModel ticker.
     */
    fun tick()

    /** Acknowledges a triggered alarm and resets [alarmTriggered] to `false`. */
    fun dismissAlarm()
}

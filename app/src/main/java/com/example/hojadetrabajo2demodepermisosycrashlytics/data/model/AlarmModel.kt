package com.example.hojadetrabajo2demodepermisosycrashlytics.data.model

/**
 * Immutable data model that represents an alarm configuration.
 *
 * @property hour     Hour at which the alarm should trigger (0-23).
 * @property minute   Minute at which the alarm should trigger (0-59).
 * @property isActive Whether the alarm is currently active (set and counting).
 */
data class AlarmModel(
    val hour: Int = 0,
    val minute: Int = 0,
    val isActive: Boolean = false
)

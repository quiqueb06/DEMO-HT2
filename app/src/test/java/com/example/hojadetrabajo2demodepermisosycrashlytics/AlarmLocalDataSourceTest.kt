package com.example.hojadetrabajo2demodepermisosycrashlytics

import com.example.hojadetrabajo2demodepermisosycrashlytics.data.datasource.AlarmLocalDataSource
import com.example.hojadetrabajo2demodepermisosycrashlytics.data.model.AlarmModel
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class AlarmLocalDataSourceTest {
    private fun time(hour: Int, minute: Int, second: Int = 0): Long = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 29, hour, minute, second)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test fun firesAtDeadlineAndDoesNotReactivate() {
        var now = time(10, 0)
        val source = AlarmLocalDataSource { now }
        source.saveAlarm(AlarmModel(10, 1, true))
        now = time(10, 0, 59)
        source.refreshCurrentTime()
        assertFalse(source.alarmTriggered.value)
        now = time(10, 1)
        source.refreshCurrentTime()
        assertTrue(source.alarmTriggered.value)
        assertFalse(source.alarm.value.isActive)
        source.acknowledgeAlarm()
        source.refreshCurrentTime()
        assertFalse(source.alarmTriggered.value)
    }

    @Test fun catchesDeadlineEvenWhenMinuteWasSkipped() {
        var now = time(10, 0)
        val source = AlarmLocalDataSource { now }
        source.saveAlarm(AlarmModel(10, 1, true))
        now = time(10, 3)
        source.refreshCurrentTime()
        assertTrue(source.alarmTriggered.value)
    }

    @Test fun pastOrCurrentMinuteUsesTomorrow() {
        val now = time(23, 59, 30)
        val source = AlarmLocalDataSource { now }
        source.saveAlarm(AlarmModel(23, 59, true))
        assertEquals(Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.SECOND, 0)
        }.timeInMillis, source.alarm.value.triggerAtMillis)
    }

    @Test fun midnightUsesNextDay() {
        var now = time(23, 59, 59)
        val source = AlarmLocalDataSource { now }
        source.saveAlarm(AlarmModel(0, 0, true))
        now += 1_000
        source.refreshCurrentTime()
        assertTrue(source.alarmTriggered.value)
        assertEquals("00:00:00", source.currentTime.value)
    }

    @Test fun cancelledAlarmNeverFires() {
        var now = time(10, 0)
        val source = AlarmLocalDataSource { now }
        source.saveAlarm(AlarmModel(10, 1, true))
        source.cancelAlarm()
        now = time(10, 2)
        source.refreshCurrentTime()
        assertFalse(source.alarmTriggered.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidTime() {
        AlarmLocalDataSource().saveAlarm(AlarmModel(24, 60, true))
    }
}

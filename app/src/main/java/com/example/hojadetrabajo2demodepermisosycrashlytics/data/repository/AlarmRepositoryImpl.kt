package com.example.hojadetrabajo2demodepermisosycrashlytics.data.repository

import com.example.hojadetrabajo2demodepermisosycrashlytics.data.datasource.AlarmLocalDataSource
import com.example.hojadetrabajo2demodepermisosycrashlytics.data.model.AlarmModel
import kotlinx.coroutines.flow.StateFlow

/**
 * Concrete implementation of [AlarmRepository] backed by [AlarmLocalDataSource].
 * Acts as the single source of truth; delegates all mutations and queries to the
 * data source so that upper layers (ViewModel) never touch raw data APIs.
 */
class AlarmRepositoryImpl(
    private val dataSource: AlarmLocalDataSource
) : AlarmRepository {

    override val alarm: StateFlow<AlarmModel> = dataSource.alarm
    override val currentTime: StateFlow<String> = dataSource.currentTime
    override val alarmTriggered: StateFlow<Boolean> = dataSource.alarmTriggered

    override fun setAlarm(hour: Int, minute: Int) {
        dataSource.saveAlarm(AlarmModel(hour = hour, minute = minute, isActive = true))
    }

    override fun cancelAlarm() {
        dataSource.cancelAlarm()
    }

    override fun tick() {
        dataSource.refreshCurrentTime()
    }

    override fun dismissAlarm() {
        dataSource.acknowledgeAlarm()
    }
}

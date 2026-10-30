package com.example.hojadetrabajo2demodepermisosycrashlytics.data.repository

import com.example.hojadetrabajo2demodepermisosycrashlytics.data.datasource.PermissionDataSource
import com.example.hojadetrabajo2demodepermisosycrashlytics.domain.permission.NotificationPermissionStatus

class PermissionRepositoryImpl(private val dataSource: PermissionDataSource) : PermissionRepository {
    override val notificationRuntimePermission: String?
        get() = dataSource.notificationRuntimePermission

    override fun isGranted(permission: String) = dataSource.isGranted(permission)

    override fun refresh(permission: String?, canShowRationale: Boolean) = classify(
        permission, permission == null || dataSource.isGranted(permission), canShowRationale
    )

    override fun onResult(permission: String?, granted: Boolean, canShowRationale: Boolean) =
        classify(permission, granted || permission == null, canShowRationale)

    private fun classify(permission: String?, granted: Boolean, rationale: Boolean): NotificationPermissionStatus {
        if (permission == null) return NotificationPermissionStatus.GRANTED
        if (granted) {
            dataSource.setRationaleObserved(permission, false)
            return NotificationPermissionStatus.GRANTED
        }
        if (rationale) {
            dataSource.setRationaleObserved(permission, true)
            return NotificationPermissionStatus.DENIED
        }
        // No rationale can also mean first request or dismissed dialog.
        return if (dataSource.wasRationaleObserved(permission))
            NotificationPermissionStatus.PERMANENTLY_DENIED else NotificationPermissionStatus.DENIED
    }
}

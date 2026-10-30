package com.example.hojadetrabajo2demodepermisosycrashlytics.data.repository

import com.example.hojadetrabajo2demodepermisosycrashlytics.domain.permission.NotificationPermissionStatus

/** Owns permission policy; never stores an Activity or launches system UI. */
interface PermissionRepository {
    val notificationRuntimePermission: String?
    fun isGranted(permission: String): Boolean
    fun refresh(permission: String?, canShowRationale: Boolean): NotificationPermissionStatus
    fun onResult(permission: String?, granted: Boolean, canShowRationale: Boolean): NotificationPermissionStatus
}

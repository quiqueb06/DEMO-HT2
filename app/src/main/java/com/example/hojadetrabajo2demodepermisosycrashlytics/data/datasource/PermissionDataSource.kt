package com.example.hojadetrabajo2demodepermisosycrashlytics.data.datasource

/** Platform permission checks and persistent history, replaceable in unit tests. */
interface PermissionDataSource {
    val notificationRuntimePermission: String?
    fun isGranted(permission: String): Boolean
    fun wasRationaleObserved(permission: String): Boolean
    fun setRationaleObserved(permission: String, observed: Boolean)
}

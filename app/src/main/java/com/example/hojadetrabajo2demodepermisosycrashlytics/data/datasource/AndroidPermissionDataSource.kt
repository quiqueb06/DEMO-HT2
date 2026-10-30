package com.example.hojadetrabajo2demodepermisosycrashlytics.data.datasource

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class AndroidPermissionDataSource(context: Context) : PermissionDataSource {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("runtime_permissions", Context.MODE_PRIVATE)
    override val notificationRuntimePermission: String?
        get() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null

    override fun isGranted(permission: String): Boolean {
        if (permission == Manifest.permission.POST_NOTIFICATIONS && Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun wasRationaleObserved(permission: String) = preferences.getBoolean("$permission.rationale", false)

    override fun setRationaleObserved(permission: String, observed: Boolean) {
        preferences.edit().putBoolean("$permission.rationale", observed).apply()
    }
}

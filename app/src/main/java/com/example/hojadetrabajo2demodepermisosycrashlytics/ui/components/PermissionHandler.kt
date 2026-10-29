package com.example.hojadetrabajo2demodepermisosycrashlytics.ui.components

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Generic runtime permission bridge. Only an explicit request opens the OS dialog.
 * A false rationale alone is ambiguous (first request or dismissed dialog), so remember
 * a previously observed rationale before treating a subsequent denial as permanent.
 */
@Composable
fun PermissionHandler(
    permission: String,
    shouldRequest: Boolean,
    onConsumed: () -> Unit,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    onPermanentlyDenied: () -> Unit,
    onStatusChanged: (granted: Boolean, permanentlyDenied: Boolean) -> Unit,
    onError: (Throwable) -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val owner = LocalLifecycleOwner.current
    val preferences = remember(context) { context.getSharedPreferences("runtime_permissions", Context.MODE_PRIVATE) }
    val currentGranted by rememberUpdatedState(onGranted)
    val currentDenied by rememberUpdatedState(onDenied)
    val currentPermanent by rememberUpdatedState(onPermanentlyDenied)
    val currentStatus by rememberUpdatedState(onStatusChanged)
    val currentError by rememberUpdatedState(onError)

    fun rationale() = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, permission) } ?: false
    fun granted() = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    fun permanent() = !granted() && !rationale() && preferences.getBoolean("$permission.rationale", false)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) {
            preferences.edit().remove("$permission.rationale").apply()
            currentGranted()
        } else {
            val canAskAgain = rationale()
            if (canAskAgain) preferences.edit().putBoolean("$permission.rationale", true).apply()
            if (activity != null && permanent()) currentPermanent() else currentDenied()
        }
    }

    DisposableEffect(owner, permission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                try {
                    if (granted()) preferences.edit().remove("$permission.rationale").apply()
                    else if (rationale()) preferences.edit().putBoolean("$permission.rationale", true).apply()
                    currentStatus(granted(), permanent())
                } catch (error: Exception) { currentError(error) }
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(shouldRequest, permission) {
        if (shouldRequest) {
            try {
                checkNotNull(activity) { "Runtime permission requires an Activity" }
                when {
                    granted() -> currentGranted()
                    permanent() -> currentPermanent()
                    else -> {
                        if (rationale()) preferences.edit().putBoolean("$permission.rationale", true).apply()
                        launcher.launch(permission)
                    }
                }
            } catch (error: Exception) { currentError(error) }
            finally { onConsumed() }
        }
    }
}

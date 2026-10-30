package com.example.hojadetrabajo2demodepermisosycrashlytics.platform.permissions

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Generic Android UI bridge only: launch and return raw observations.
 * No permission checks, persistence, API-version policy or denial classification.
 */
@Composable
fun PermissionHandler(
    permission: String?,
    shouldRequest: Boolean,
    onConsumed: () -> Unit,
    onResult: (granted: Boolean, canShowRationale: Boolean) -> Unit,
    onObserved: (canShowRationale: Boolean) -> Unit,
    onError: (Throwable) -> Unit
) {
    val activity = LocalActivity.current
    val owner = LocalLifecycleOwner.current
    val currentResult by rememberUpdatedState(onResult)
    val currentObserved by rememberUpdatedState(onObserved)
    val currentError by rememberUpdatedState(onError)
    fun rationale() = if (permission != null && activity != null)
        ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) else false

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        try { currentResult(granted, rationale()) }
        catch (error: Exception) { currentError(error) }
    }
    DisposableEffect(owner, permission, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                try { currentObserved(rationale()) }
                catch (error: Exception) { currentError(error) }
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(shouldRequest, permission) {
        if (shouldRequest) {
            try {
                checkNotNull(activity) { "Runtime permission requires an Activity" }
                launcher.launch(checkNotNull(permission) { "No runtime permission to request" })
            } catch (error: Exception) { currentError(error) }
            finally { onConsumed() }
        }
    }
}

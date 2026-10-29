package com.example.hojadetrabajo2demodepermisosycrashlytics.ui.components

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Generic, reusable composable that manages a single runtime permission.
 *
 * ## Usage
 * Place this composable anywhere in the tree (it renders nothing itself).
 * Control the request lifecycle via [shouldRequest] — when it flips to `true`,
 * the OS dialog is shown immediately and [onConsumed] is called so the caller
 * can reset the flag.
 *
 * ## Permanent denial detection
 * After each denial the component tracks whether a previous denial occurred.
 * Combined with [ActivityCompat.shouldShowRequestPermissionRationale]:
 * - `showRationale = true`   → simple denial  → [onDenied]
 * - `showRationale = false` AND previously denied → permanent denial → [onPermanentlyDenied]
 * - `showRationale = false` AND first attempt  → edge-case denial  → [onDenied]
 *
 * @param permission            Manifest permission string (e.g. `Manifest.permission.POST_NOTIFICATIONS`).
 * @param shouldRequest         When `true`, the OS dialog is launched on the next recomposition.
 * @param onConsumed            Called immediately after the dialog is launched so the caller
 *                              can set [shouldRequest] back to `false`.
 * @param onGranted             Called when the user grants the permission.
 * @param onDenied              Called when the user denies but can be asked again.
 * @param onPermanentlyDenied   Called when the user has permanently denied the permission.
 */
@Composable
fun PermissionHandler(
    permission: String,
    shouldRequest: Boolean,
    onConsumed: () -> Unit,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    onPermanentlyDenied: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Survives configuration changes via rememberSaveable
    var wasAlreadyDeniedBefore by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onGranted()
        } else {
            val canShowRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
            } ?: false

            when {
                canShowRationale -> {
                    // System will show "allow" button again → simple denial
                    wasAlreadyDeniedBefore = true
                    onDenied()
                }
                wasAlreadyDeniedBefore -> {
                    // showRationale=false after a previous denial → "Don't ask again" was selected
                    onPermanentlyDenied()
                }
                else -> {
                    // First denial on a device where showRationale stays false (uncommon)
                    wasAlreadyDeniedBefore = true
                    onDenied()
                }
            }
        }
    }

    // React to shouldRequest transitions: launch dialog then immediately reset the flag
    LaunchedEffect(shouldRequest) {
        if (shouldRequest) {
            // Fast-path: if already granted, skip the OS dialog
            if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED
            ) {
                onGranted()
            } else {
                launcher.launch(permission)
            }
            onConsumed()
        }
    }
}

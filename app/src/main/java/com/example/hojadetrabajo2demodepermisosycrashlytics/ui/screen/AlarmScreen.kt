package com.example.hojadetrabajo2demodepermisosycrashlytics.ui.screen

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Switch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.hojadetrabajo2demodepermisosycrashlytics.mvi.AlarmEffect
import com.example.hojadetrabajo2demodepermisosycrashlytics.mvi.AlarmIntent
import com.example.hojadetrabajo2demodepermisosycrashlytics.mvi.AlarmUiState
import com.example.hojadetrabajo2demodepermisosycrashlytics.mvi.NotificationPermissionStatus
import com.example.hojadetrabajo2demodepermisosycrashlytics.ui.components.PermissionHandler
import com.example.hojadetrabajo2demodepermisosycrashlytics.ui.components.SheepJumpingAnimation
import com.example.hojadetrabajo2demodepermisosycrashlytics.viewmodel.AlarmViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Colour tokens
// ─────────────────────────────────────────────────────────────────────────────
private val DarkBg        = Color(0xFF0D1B2A)
private val CardBg        = Color(0xFF1B2D42)
private val AccentTeal    = Color(0xFF26C6DA)
private val AccentGreen   = Color(0xFF66BB6A)
private val AccentAmber   = Color(0xFFFFB300)
private val DangerRed     = Color(0xFFEF5350)
private val TextPrimary   = Color(0xFFECEFF1)
private val TextSecondary = Color(0xFF90A4AE)

/**
 * Root MVI screen composable for the Sheep Alarm app.
 *
 * ## MVI rules observed:
 * - Reads state exclusively from [AlarmViewModel.uiState] via lifecycle-aware collection.
 * - Sends actions exclusively via [AlarmViewModel.processIntent].
 * - One-shot effects are consumed via [LaunchedEffect] on [AlarmViewModel.effects].
 * - Never imports or references [AlarmRepository], data sources, or models.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(viewModel: AlarmViewModel) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ── Permission request trigger (driven by LaunchNotificationPermission effect) ──
    var shouldRequestPermission by remember { mutableStateOf(false) }

    // ── Consume one-shot effects ──────────────────────────────────────────────
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AlarmEffect.LaunchNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= 33) shouldRequestPermission = true
                    else viewModel.processIntent(AlarmIntent.OpenSettings)
                }
                AlarmEffect.OpenAppSettings -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    try { context.startActivity(intent) }
                    catch (error: Exception) { viewModel.processIntent(AlarmIntent.ReportError(error)) }
                }
            }
        }
    }

    // ── Generic POST_NOTIFICATIONS permission handler (Android 13+ only) ─────
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PermissionHandler(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            shouldRequest = shouldRequestPermission,
            onConsumed = { shouldRequestPermission = false },
            onGranted = {
                viewModel.processIntent(
                    AlarmIntent.NotificationPermissionResult(granted = true, permanentlyDenied = false)
                )
            },
            onDenied = {
                viewModel.processIntent(
                    AlarmIntent.NotificationPermissionResult(granted = false, permanentlyDenied = false)
                )
            },
            onStatusChanged = { granted, permanent ->
                viewModel.processIntent(AlarmIntent.SyncNotificationPermission(granted, permanent))
            },
            onError = { viewModel.processIntent(AlarmIntent.ReportError(it)) },
            onPermanentlyDenied = {
                viewModel.processIntent(
                    AlarmIntent.NotificationPermissionResult(granted = false, permanentlyDenied = true)
                )
            }
        )
    }

    if (Build.VERSION.SDK_INT < 33) {
        LifecycleResumeEffect(Unit) {
            viewModel.processIntent(AlarmIntent.SyncNotificationPermission(true, false))
            onPauseOrDispose { }
        }
    }

    // ── Root layout ────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBg, Color(0xFF0A2540))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))

            // Title
            Text("Sheep Alarm", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("Count sheep until it's time!", color = TextSecondary, fontSize = 14.sp)

            Spacer(Modifier.height(24.dp))

            // Current time card
            CurrentTimeCard(state)

            Spacer(Modifier.height(16.dp))

            // Sheep animation card
            SheepAnimationCard(state)

            Spacer(Modifier.height(16.dp))

            // Alarm status card
            AlarmStatusCard(state)

            Spacer(Modifier.height(16.dp))

            // Notification permission banner (shown when denied/permanently denied)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Avisos de alarma", color = TextPrimary, modifier = Modifier.weight(1f))
                Switch(modifier = Modifier.semantics { contentDescription = "Avisos de alarma" },
                    checked = state.notificationsEnabled, onCheckedChange = {
                    viewModel.processIntent(AlarmIntent.SetNotificationsEnabled(it))
                })
            }
            if (state.notificationsEnabled) NotificationPermissionBanner(state, viewModel)
            state.errorMessage?.let { Text(it, color = DangerRed) }

            Spacer(Modifier.height(16.dp))

            // Action buttons
            ActionButtons(state, viewModel)
            Spacer(Modifier.height(24.dp))
        }

        // Alarm triggered overlay
        AnimatedVisibility(
            visible = state.isAlarmTriggered,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            AlarmTriggeredOverlay(onDismiss = { viewModel.processIntent(AlarmIntent.DismissAlarm) })
        }

        // Time picker dialog
        if (state.showTimePicker) {
            TimePickerDialog(
                onConfirm = { h, m -> viewModel.processIntent(AlarmIntent.SetAlarm(h, m)) },
                onDismiss = { viewModel.processIntent(AlarmIntent.HideTimePicker) }
            )
        }

        // Notification rationale dialog
        if (state.showNotificationRationale) {
            NotificationRationaleDialog(
                isPermanent = state.notificationPermission == NotificationPermissionStatus.PERMANENTLY_DENIED,
                onRequestAgain = { viewModel.processIntent(AlarmIntent.RequestNotificationPermission) },
                onOpenSettings = { viewModel.processIntent(AlarmIntent.OpenSettings) },
                onDismiss = { viewModel.processIntent(AlarmIntent.DismissNotificationRationale) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CurrentTimeCard(state: AlarmUiState) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("CURRENT TIME", color = TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Text(state.currentTime, color = AccentTeal, fontSize = 46.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SheepAnimationCard(state: AlarmUiState) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF163A2C)),
        modifier = Modifier.fillMaxWidth()
    ) {
        SheepJumpingAnimation(
            timeLabel = state.currentTime,
            isJumping = state.isSheepJumping,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
private fun AlarmStatusCard(state: AlarmUiState) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "dot_pulse"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isAlarmSet) Color(0xFF1A3A2A) else CardBg
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    if (state.isAlarmSet) "Alarm Set" else "No Alarm Active",
                    color = if (state.isAlarmSet) AccentGreen else TextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (state.isAlarmSet) {
                    Text(
                        "%02d:%02d".format(state.alarmHour, state.alarmMinute),
                        color = TextPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Box(
                Modifier
                    .size(18.dp)
                    .scale(if (state.isAlarmSet) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(if (state.isAlarmSet) AccentGreen else Color(0xFF37474F))
            )
        }
    }
}

@Composable
private fun NotificationPermissionBanner(state: AlarmUiState, viewModel: AlarmViewModel) {
    when (state.notificationPermission) {
        NotificationPermissionStatus.DENIED -> {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2010)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "🔔 Notifications disabled",
                        color = AccentAmber,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.processIntent(AlarmIntent.RequestNotificationPermission) }) {
                        Text("Enable", color = AccentAmber, fontSize = 13.sp)
                    }
                }
            }
        }
        NotificationPermissionStatus.PERMANENTLY_DENIED -> {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1010)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "🔕 Notifications blocked\n(animation still works!)",
                        color = DangerRed,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.processIntent(AlarmIntent.OpenSettings) }) {
                        Text("Settings", color = DangerRed, fontSize = 13.sp)
                    }
                }
            }
        }
        else -> { /* No banner needed */ }
    }
}

@Composable
private fun ActionButtons(state: AlarmUiState, viewModel: AlarmViewModel) {
    Button(
        onClick = { viewModel.processIntent(AlarmIntent.ShowTimePicker) },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Text(
            if (state.isAlarmSet) "Change Alarm" else "Set Alarm",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }

    if (state.isAlarmSet) {
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { viewModel.processIntent(AlarmIntent.CancelAlarm) },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Cancel Alarm", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberTimePickerState(is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Pick Alarm Time", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                TimePicker(
                    state = pickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = DarkBg,
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = TextSecondary,
                        selectorColor = AccentTeal,
                        timeSelectorSelectedContainerColor = AccentTeal,
                        timeSelectorUnselectedContainerColor = Color(0xFF1E3A50),
                        timeSelectorSelectedContentColor = Color.White,
                        timeSelectorUnselectedContentColor = TextSecondary
                    )
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) { Text("Cancel") }
                    Button(
                        onClick = { onConfirm(pickerState.hour, pickerState.minute) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) { Text("Set", color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun NotificationRationaleDialog(
    isPermanent: Boolean,
    onRequestAgain: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔔", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    if (isPermanent) "Notifications Blocked" else "Enable Notifications?",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isPermanent)
                        "You've permanently blocked notifications. Open Settings to allow them.\n\n⚠️ The sheep animation will still work without notifications."
                    else
                        "Allow notifications so you get an alert when the alarm fires.\n\n⚠️ The sheep animation works regardless of this permission.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                if (isPermanent) {
                    Button(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Open Settings", color = Color.Black, fontWeight = FontWeight.Bold) }
                } else {
                    Button(
                        onClick = onRequestAgain,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Allow Notifications", color = Color.White, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Keep animation only", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun AlarmTriggeredOverlay(onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "alarm_glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.65f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "overlay_alpha"
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2D42)),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⏰", fontSize = 72.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Wake Up!",
                    color = Color(0xFFFFEB3B),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "The sheep stopped jumping.\nAlarm time reached!",
                    color = TextSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Dismiss", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

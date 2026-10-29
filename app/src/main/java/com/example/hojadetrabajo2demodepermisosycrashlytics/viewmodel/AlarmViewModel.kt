package com.example.hojadetrabajo2demodepermisosycrashlytics.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hojadetrabajo2demodepermisosycrashlytics.data.datasource.AlarmLocalDataSource
import com.example.hojadetrabajo2demodepermisosycrashlytics.data.repository.AlarmRepository
import com.example.hojadetrabajo2demodepermisosycrashlytics.data.repository.AlarmRepositoryImpl
import com.example.hojadetrabajo2demodepermisosycrashlytics.domain.crash.CrashReporter
import com.example.hojadetrabajo2demodepermisosycrashlytics.domain.crash.LogCrashReporter
import com.example.hojadetrabajo2demodepermisosycrashlytics.mvi.AlarmEffect
import com.example.hojadetrabajo2demodepermisosycrashlytics.mvi.AlarmIntent
import com.example.hojadetrabajo2demodepermisosycrashlytics.mvi.AlarmUiState
import com.example.hojadetrabajo2demodepermisosycrashlytics.mvi.NotificationPermissionStatus
import com.example.hojadetrabajo2demodepermisosycrashlytics.notification.AlarmNotificationManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI ViewModel for the Sheep Alarm screen.
 *
 * ## MVI contract
 * - **State**   → [uiState] — single immutable [AlarmUiState], updated atomically.
 * - **Intents** → [processIntent] — single public entry point for ALL user actions.
 * - **Effects** → [effects]  — one-shot [AlarmEffect] events consumed by the UI.
 *
 * ## Design decisions
 * - Extends [AndroidViewModel] to access [Application] context for notifications.
 * - [AlarmRepository] is typed as the interface so it can be replaced in tests.
 * - [CrashReporter] is injected so all caught exceptions are centrally reported.
 * - The UI layer NEVER accesses [AlarmRepository], [AlarmLocalDataSource], or
 *   [AlarmNotificationManager] directly.
 */
class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    // -------------------------------------------------------------------------
    // Dependencies (in a production app these would be injected via Hilt)
    // -------------------------------------------------------------------------
    private val crashReporter: CrashReporter = LogCrashReporter()
    private val repository: AlarmRepository = AlarmRepositoryImpl(AlarmLocalDataSource())
    private val notificationManager = AlarmNotificationManager(application)

    // -------------------------------------------------------------------------
    // MVI State — private mutable, public read-only
    // -------------------------------------------------------------------------
    private val _uiState = MutableStateFlow(AlarmUiState())
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // MVI Effects — buffered Channel so effects are not lost on slow collectors
    // -------------------------------------------------------------------------
    private val _effects = Channel<AlarmEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // -------------------------------------------------------------------------
    // Init: start ticker + observe alarm-triggered signal
    // -------------------------------------------------------------------------
    init {
        startTicker()
        observeAlarmTriggered()
        observeCurrentTime()
    }

    /**
     * One-second ticker that drives [AlarmRepository.tick].
     * The data source will update currentTime and, when the alarm time is reached,
     * set alarmTriggered to true — which is observed in [observeAlarmTriggered].
     */
    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                try {
                    repository.tick()
                } catch (e: Exception) {
                    crashReporter.report(e)
                }
                delay(1_000L)
            }
        }
    }

    /** Syncs [AlarmUiState.currentTime] with the repository stream. */
    private fun observeCurrentTime() {
        viewModelScope.launch {
            repository.currentTime.collect { time ->
                updateState { it.copy(currentTime = time) }
            }
        }
    }

    /** When the alarm fires, update state and emit the notification effect. */
    private fun observeAlarmTriggered() {
        viewModelScope.launch {
            repository.alarmTriggered.collect { triggered ->
                if (triggered) {
                    crashReporter.log("Alarm triggered — updating state and emitting effect")
                    updateState {
                        it.copy(
                            isAlarmSet = false,
                            isAlarmTriggered = true,
                            isSheepJumping = false
                        )
                    }
                    // Only show notification if permission is granted (or API < 33)
                    if (hasNotificationPermission()) {
                        emitEffect(AlarmEffect.TriggerAlarmNotification)
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Public MVI entry point
    // -------------------------------------------------------------------------

    /**
     * Processes an [AlarmIntent] dispatched from the UI.
     * All logic is funnelled through this single function (MVI rule).
     */
    fun processIntent(intent: AlarmIntent) {
        viewModelScope.launch {
            try {
                crashReporter.log("Intent received: ${intent::class.simpleName}")
                when (intent) {
                    is AlarmIntent.SetAlarm              -> handleSetAlarm(intent.hour, intent.minute)
                    AlarmIntent.CancelAlarm              -> handleCancelAlarm()
                    AlarmIntent.DismissAlarm             -> handleDismissAlarm()
                    AlarmIntent.ShowTimePicker           -> updateState { it.copy(showTimePicker = true) }
                    AlarmIntent.HideTimePicker           -> updateState { it.copy(showTimePicker = false) }
                    AlarmIntent.RequestNotificationPermission -> handleRequestPermission()
                    is AlarmIntent.NotificationPermissionResult -> handlePermissionResult(intent)
                    AlarmIntent.OpenSettings             -> emitEffect(AlarmEffect.OpenAppSettings)
                    AlarmIntent.DismissNotificationRationale  -> updateState { it.copy(showNotificationRationale = false) }
                }
            } catch (e: Exception) {
                crashReporter.report(e)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Intent handlers (private)
    // -------------------------------------------------------------------------

    private suspend fun handleSetAlarm(hour: Int, minute: Int) {
        repository.setAlarm(hour, minute)
        updateState {
            it.copy(
                alarmHour = hour,
                alarmMinute = minute,
                isAlarmSet = true,
                isAlarmTriggered = false,
                isSheepJumping = true,
                showTimePicker = false
            )
        }
        // On Android 13+, request notification permission if not yet granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission() &&
                _uiState.value.notificationPermission == NotificationPermissionStatus.NOT_REQUESTED
            ) {
                emitEffect(AlarmEffect.LaunchNotificationPermission)
            }
        } else {
            // API < 33: notifications are always allowed
            updateState { it.copy(notificationPermission = NotificationPermissionStatus.GRANTED) }
        }
    }

    private fun handleCancelAlarm() {
        repository.cancelAlarm()
        updateState {
            it.copy(
                isAlarmSet = false,
                isAlarmTriggered = false,
                isSheepJumping = false
            )
        }
        emitEffect(AlarmEffect.CancelAlarmNotification)
    }

    private fun handleDismissAlarm() {
        repository.dismissAlarm()
        updateState {
            it.copy(
                isAlarmSet = false,
                isAlarmTriggered = false,
                isSheepJumping = false
            )
        }
        emitEffect(AlarmEffect.CancelAlarmNotification)
    }

    private suspend fun handleRequestPermission() {
        if (hasNotificationPermission()) {
            updateState { it.copy(notificationPermission = NotificationPermissionStatus.GRANTED) }
        } else {
            updateState { it.copy(showNotificationRationale = false) }
            emitEffect(AlarmEffect.LaunchNotificationPermission)
        }
    }

    private suspend fun handlePermissionResult(intent: AlarmIntent.NotificationPermissionResult) {
        when {
            intent.granted -> {
                updateState { it.copy(notificationPermission = NotificationPermissionStatus.GRANTED) }
                crashReporter.log("POST_NOTIFICATIONS permission granted")
            }
            intent.permanentlyDenied -> {
                updateState {
                    it.copy(
                        notificationPermission = NotificationPermissionStatus.PERMANENTLY_DENIED,
                        showNotificationRationale = true
                    )
                }
                crashReporter.log("POST_NOTIFICATIONS permanently denied")
            }
            else -> {
                updateState {
                    it.copy(
                        notificationPermission = NotificationPermissionStatus.DENIED,
                        showNotificationRationale = true
                    )
                }
                crashReporter.log("POST_NOTIFICATIONS denied (can ask again)")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Notification helpers — called from the UI's effect collector
    // -------------------------------------------------------------------------

    /** Posts the alarm notification. Call in response to [AlarmEffect.TriggerAlarmNotification]. */
    fun showAlarmNotification() {
        notificationManager.showAlarmNotification().onFailure { e ->
            crashReporter.report(e)
        }
    }

    /** Cancels the alarm notification. Call in response to [AlarmEffect.CancelAlarmNotification]. */
    fun cancelAlarmNotification() {
        notificationManager.cancelNotification()
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /** Thread-safe atomic state update. */
    private fun updateState(reducer: (AlarmUiState) -> AlarmUiState) {
        _uiState.update(reducer)
    }

    /** Sends an effect to the buffered channel. Never throws. */
    private fun emitEffect(effect: AlarmEffect) {
        _effects.trySend(effect)
    }
}

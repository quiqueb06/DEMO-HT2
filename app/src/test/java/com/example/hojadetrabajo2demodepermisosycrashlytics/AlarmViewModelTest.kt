package com.example.hojadetrabajo2demodepermisosycrashlytics

import androidx.lifecycle.ViewModelStore
import com.example.hojadetrabajo2demodepermisosycrashlytics.data.datasource.PermissionDataSource
import com.example.hojadetrabajo2demodepermisosycrashlytics.data.repository.PermissionRepositoryImpl
import com.example.hojadetrabajo2demodepermisosycrashlytics.domain.permission.NotificationPermissionStatus
import com.example.hojadetrabajo2demodepermisosycrashlytics.data.datasource.AlarmLocalDataSource
import com.example.hojadetrabajo2demodepermisosycrashlytics.data.repository.AlarmRepositoryImpl
import com.example.hojadetrabajo2demodepermisosycrashlytics.domain.crash.CrashReporter
import com.example.hojadetrabajo2demodepermisosycrashlytics.mvi.*
import com.example.hojadetrabajo2demodepermisosycrashlytics.notification.AlarmNotifier
import com.example.hojadetrabajo2demodepermisosycrashlytics.viewmodel.AlarmViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var now = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 29, 10, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private val source = AlarmLocalDataSource { now }
    private val reporter = RecordingReporter()
    private val notifier = FakeNotifier()
    private val permissionSource = FakePermissionSource { notifier.available }
    private lateinit var vm: AlarmViewModel

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        vm = AlarmViewModel(AlarmRepositoryImpl(source), notifier, reporter, PermissionRepositoryImpl(permissionSource))
        store.put("alarm", vm)
    }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    @Test fun settingAlarmNeverRequestsNotificationPermission() = vmTest {
        val effects = mutableListOf<AlarmEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.toList(effects) }
        vm.processIntent(AlarmIntent.SetAlarm(10, 1))
        runCurrent()
        assertTrue(vm.uiState.value.isSheepJumping)
        assertFalse(vm.uiState.value.notificationsEnabled)
        assertTrue(effects.isEmpty())
        vm.processIntent(AlarmIntent.SetNotificationsEnabled(true))
        runCurrent()
        assertEquals(listOf(AlarmEffect.LaunchNotificationPermission), effects)
    }

    @Test fun bothDenialsKeepSheepJumping() = vmTest {
        vm.processIntent(AlarmIntent.SetAlarm(10, 1))
        vm.processIntent(AlarmIntent.NotificationPermissionResult(false, true))
        runCurrent()
        assertEquals(NotificationPermissionStatus.DENIED, vm.uiState.value.notificationPermission)
        assertTrue(vm.uiState.value.isSheepJumping)
        vm.processIntent(AlarmIntent.NotificationPermissionResult(false, false))
        runCurrent()
        assertEquals(NotificationPermissionStatus.PERMANENTLY_DENIED, vm.uiState.value.notificationPermission)
        assertTrue(vm.uiState.value.isSheepJumping)
        assertTrue(reporter.errors.isEmpty())
    }

    @Test fun triggerStopsSheepAndNotifiesExactlyOnce() = vmTest {
        notifier.available = true
        vm.processIntent(AlarmIntent.SetNotificationsEnabled(true))
        vm.processIntent(AlarmIntent.SetAlarm(10, 1))
        runCurrent()
        now += 60_000
        source.refreshCurrentTime()
        runCurrent()
        assertFalse(vm.uiState.value.isSheepJumping)
        assertTrue(vm.uiState.value.isAlarmTriggered)
        assertEquals(1, notifier.deliveries)
        source.refreshCurrentTime()
        runCurrent()
        assertEquals(1, notifier.deliveries)
    }

    @Test fun noPermissionStillTriggersWithoutNotification() = vmTest {
        vm.processIntent(AlarmIntent.SetNotificationsEnabled(true))
        vm.processIntent(AlarmIntent.SetAlarm(10, 1))
        runCurrent()
        now += 60_000
        source.refreshCurrentTime()
        runCurrent()
        assertTrue(vm.uiState.value.isAlarmTriggered)
        assertFalse(vm.uiState.value.isSheepJumping)
        assertEquals(0, notifier.deliveries)
    }

    @Test fun preferenceOffSuppressesNotificationEvenWithPermission() = vmTest {
        notifier.available = true
        vm.processIntent(AlarmIntent.SetNotificationsEnabled(true))
        vm.processIntent(AlarmIntent.SetNotificationsEnabled(false))
        vm.processIntent(AlarmIntent.SetAlarm(10, 1))
        runCurrent()
        now += 60_000
        source.refreshCurrentTime()
        runCurrent()
        assertEquals(0, notifier.deliveries)
        assertTrue(vm.uiState.value.isAlarmTriggered)
    }

    @Test fun settingsRefreshDoesNotShowRationaleAndUpdatesPermission() = vmTest {
        vm.processIntent(AlarmIntent.SyncNotificationPermission(false))
        runCurrent()
        assertFalse(vm.uiState.value.showNotificationRationale)
        notifier.available = true
        vm.processIntent(AlarmIntent.SyncNotificationPermission(false))
        runCurrent()
        assertEquals(NotificationPermissionStatus.GRANTED, vm.uiState.value.notificationPermission)
    }

    @Test fun invalidTimeReportsErrorWithoutStartingSheep() = vmTest {
        vm.processIntent(AlarmIntent.SetAlarm(-1, 60))
        runCurrent()
        assertEquals(1, reporter.errors.size)
        assertFalse(vm.uiState.value.isSheepJumping)
        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test fun notificationFailureReportsErrorButAlarmRemainsTriggered() = vmTest {
        notifier.available = true
        notifier.failure = IllegalStateException("delivery failed")
        vm.processIntent(AlarmIntent.SetNotificationsEnabled(true))
        vm.processIntent(AlarmIntent.SetAlarm(10, 1))
        runCurrent()
        now += 60_000
        source.refreshCurrentTime()
        runCurrent()
        assertTrue(vm.uiState.value.isAlarmTriggered)
        assertFalse(vm.uiState.value.isSheepJumping)
        assertEquals(notifier.failure, reporter.errors.single())
    }

    @Test fun cancellationAndDismissalResetState() = vmTest {
        vm.processIntent(AlarmIntent.SetAlarm(10, 1))
        runCurrent()
        vm.processIntent(AlarmIntent.CancelAlarm)
        runCurrent()
        assertFalse(vm.uiState.value.isSheepJumping)
        vm.processIntent(AlarmIntent.SetAlarm(10, 1))
        runCurrent()
        now += 60_000
        source.refreshCurrentTime()
        runCurrent()
        vm.processIntent(AlarmIntent.DismissAlarm)
        runCurrent()
        assertFalse(vm.uiState.value.isAlarmTriggered)
    }

    private fun vmTest(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try { block() } finally { store.clear() }
    }

    private class FakePermissionSource(private val granted: () -> Boolean) : PermissionDataSource {
        override val notificationRuntimePermission = "test.permission.NOTIFICATIONS"
        private var observed = false
        override fun isGranted(permission: String) = granted()
        override fun wasRationaleObserved(permission: String) = observed
        override fun setRationaleObserved(permission: String, observed: Boolean) { this.observed = observed }
    }

    private class FakeNotifier : AlarmNotifier {
        override var notificationsEnabled = false
        var available = false
        var deliveries = 0
        var failure: Throwable? = null
        override fun canNotify() = available
        override fun showAlarmNotification(): Result<Unit> {
            deliveries++
            return failure?.let { Result.failure(it) } ?: Result.success(Unit)
        }
        override fun cancelNotification() = Unit
    }
    private class RecordingReporter : CrashReporter {
        val errors = mutableListOf<Throwable>()
        override fun report(throwable: Throwable) { errors.add(throwable) }
        override fun report(message: String) = Unit
        override fun log(message: String) = Unit
    }
}

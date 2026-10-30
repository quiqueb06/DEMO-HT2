package com.example.hojadetrabajo2demodepermisosycrashlytics

import com.example.hojadetrabajo2demodepermisosycrashlytics.data.datasource.PermissionDataSource
import com.example.hojadetrabajo2demodepermisosycrashlytics.data.repository.PermissionRepositoryImpl
import com.example.hojadetrabajo2demodepermisosycrashlytics.domain.permission.NotificationPermissionStatus
import org.junit.Assert.*
import org.junit.Test

class PermissionRepositoryTest {
    private val permission = "test.permission.NOTIFICATIONS"
    private val source = FakeSource()
    private val repository = PermissionRepositoryImpl(source)

    @Test fun beforeAndroid13DoesNotRequireRuntimePermission() {
        source.notificationRuntimePermission = null
        assertEquals(NotificationPermissionStatus.GRANTED, repository.refresh(null, false))
    }
    @Test fun firstRequestOrDismissalIsNotMistakenForPermanentDenial() {
        repeat(2) {
            assertEquals(NotificationPermissionStatus.DENIED, repository.onResult(permission, false, false))
        }
    }
    @Test fun rationaleMeansOrdinaryDenial() {
        assertEquals(NotificationPermissionStatus.DENIED, repository.onResult(permission, false, true))
        assertTrue(source.wasRationaleObserved(permission))
    }
    @Test fun denialWithoutRationaleAfterPreviousRationaleIsPermanent() {
        repository.onResult(permission, false, true)
        assertEquals(NotificationPermissionStatus.PERMANENTLY_DENIED, repository.onResult(permission, false, false))
    }
    @Test fun permanentHistorySurvivesRepositoryRecreation() {
        repository.onResult(permission, false, true)
        val recreated = PermissionRepositoryImpl(source)
        assertEquals(NotificationPermissionStatus.PERMANENTLY_DENIED, recreated.refresh(permission, false))
    }
    @Test fun settingsGrantClearsDenialHistory() {
        repository.onResult(permission, false, true)
        source.granted = true
        assertEquals(NotificationPermissionStatus.GRANTED, repository.refresh(permission, false))
        assertFalse(source.wasRationaleObserved(permission))
        source.granted = false
        assertEquals(NotificationPermissionStatus.DENIED, repository.refresh(permission, false))
    }
    @Test fun grantFromDialogClearsDenialHistory() {
        repository.onResult(permission, false, true)
        assertEquals(NotificationPermissionStatus.GRANTED, repository.onResult(permission, true, false))
        assertFalse(source.wasRationaleObserved(permission))
    }
    @Test fun historiesAreIndependentForEachPermission() {
        repository.onResult(permission, false, true)
        assertEquals(NotificationPermissionStatus.DENIED, repository.refresh("other.permission", false))
    }

    private class FakeSource : PermissionDataSource {
        override var notificationRuntimePermission: String? = "test.permission.NOTIFICATIONS"
        var granted = false
        private val history = mutableMapOf<String, Boolean>()
        override fun isGranted(permission: String) = granted
        override fun wasRationaleObserved(permission: String) = history[permission] ?: false
        override fun setRationaleObserved(permission: String, observed: Boolean) { history[permission] = observed }
    }
}

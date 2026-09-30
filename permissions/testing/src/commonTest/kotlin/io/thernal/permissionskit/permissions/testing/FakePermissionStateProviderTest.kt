package io.thernal.permissionskit.permissions.testing

import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakePermissionStateProviderTest {
    @Test
    fun `an unset permission is not determined`() {
        val fake = FakePermissionStateProvider()

        assertEquals(PermissionStatus.NotDetermined, fake.status(AppPermission.Camera))
    }

    @Test
    fun `a request records the permissions and applies the answer to those not granted`() {
        val fake = FakePermissionStateProvider(
            AppPermission.Camera to PermissionStatus.Granted,
            AppPermission.Microphone to PermissionStatus.Restricted,
            answer = { PermissionStatus.Denied },
        )
        val state = fake.stateFor(listOf(AppPermission.Camera, AppPermission.Microphone, AppPermission.PhotoLibrary))

        state.request()

        assertEquals(
            listOf(listOf(AppPermission.Camera, AppPermission.Microphone, AppPermission.PhotoLibrary)),
            fake.requests,
        )
        assertEquals(
            mapOf(
                AppPermission.Camera to PermissionStatus.Granted,
                AppPermission.Microphone to PermissionStatus.Restricted,
                AppPermission.PhotoLibrary to PermissionStatus.Denied,
            ),
            state.statuses,
        )
    }

    @Test
    fun `statuses follow what the test sets`() {
        val fake = FakePermissionStateProvider(AppPermission.PhotoLibrary to PermissionStatus.Denied)
        val state = fake.stateFor(listOf(AppPermission.PhotoLibrary))
        assertFalse(state.areAllGranted)

        fake.set(AppPermission.PhotoLibrary, PermissionStatus.Limited)

        assertTrue(state.areAllGranted)
    }

    @Test
    fun `open settings is counted`() {
        val fake = FakePermissionStateProvider()

        fake.stateFor(listOf(AppPermission.Notification)).openSettings()

        assertEquals(1, fake.settingsOpened)
    }
}

package io.thernal.permissionskit.permissions.impl.presentation

import android.Manifest
import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AndroidPermissionStatusTest {
    @Test
    fun `granted wins over everything`() {
        assertEquals(
            PermissionStatus.Granted,
            androidPermissionStatus(granted = true, shouldShowRationale = true, isRefused = true),
        )
    }

    @Test
    fun `a rationale means the system asks again`() {
        assertEquals(
            PermissionStatus.ShouldShowRationale,
            androidPermissionStatus(granted = false, shouldShowRationale = true, isRefused = true),
        )
    }

    @Test
    fun `a recorded refusal without a rationale is denied for good`() {
        assertEquals(
            PermissionStatus.Denied,
            androidPermissionStatus(granted = false, shouldShowRationale = false, isRefused = true),
        )
    }

    @Test
    fun `not granted without a refusal can still be asked`() {
        // Never asked, a one-time grant that lapsed, or "Ask every time" in settings.
        assertEquals(
            PermissionStatus.NotDetermined,
            androidPermissionStatus(granted = false, shouldShowRationale = false, isRefused = false),
        )
    }

    @Test
    fun `an answer without a dialog is a refusal for good`() {
        assertTrue(
            isRefusedAfterRequest(
                granted = false,
                shouldShowRationale = false,
                isDialogShown = false,
                before = PermissionStatus.NotDetermined,
            ),
        )
    }

    @Test
    fun `refusing again after a rationale is final`() {
        assertTrue(
            isRefusedAfterRequest(
                granted = false,
                shouldShowRationale = false,
                isDialogShown = true,
                before = PermissionStatus.ShouldShowRationale,
            ),
        )
    }

    @Test
    fun `a first dialog closed without an answer is not a refusal`() {
        assertFalse(
            isRefusedAfterRequest(
                granted = false,
                shouldShowRationale = false,
                isDialogShown = true,
                before = PermissionStatus.NotDetermined,
            ),
        )
    }

    @Test
    fun `a first refusal leaves a rationale and is not final`() {
        assertFalse(
            isRefusedAfterRequest(
                granted = false,
                shouldShowRationale = true,
                isDialogShown = true,
                before = PermissionStatus.NotDetermined,
            ),
        )
    }

    @Test
    fun `a grant, one-time or not, clears any refusal`() {
        assertFalse(
            isRefusedAfterRequest(
                granted = true,
                shouldShowRationale = false,
                isDialogShown = true,
                before = PermissionStatus.Denied,
            ),
        )
    }

    @Test
    fun `a pause shorter than a person can answer is the system answering by itself`() {
        assertFalse(isDialogShown(hasPaused = true, elapsed = 115.milliseconds))
        assertFalse(isDialogShown(hasPaused = false, elapsed = 5.seconds))
        assertTrue(isDialogShown(hasPaused = true, elapsed = 800.milliseconds))
    }

    @Test
    fun `notifications ask for a permission from Android 13`() {
        assertEquals(
            listOf(Manifest.permission.POST_NOTIFICATIONS),
            AppPermission.Notification.manifestPermissions(sdk = 33),
        )
        assertTrue(AppPermission.Notification.manifestPermissions(sdk = 32).isEmpty())
    }

    @Test
    fun `the photo library never asks and the camera and microphone always do`() {
        assertTrue(AppPermission.PhotoLibrary.manifestPermissions(sdk = 36).isEmpty())
        assertEquals(listOf(Manifest.permission.CAMERA), AppPermission.Camera.manifestPermissions(sdk = 24))
        assertEquals(listOf(Manifest.permission.RECORD_AUDIO), AppPermission.Microphone.manifestPermissions(sdk = 24))
    }

    @Test
    fun `location offers both accuracies in one dialog`() {
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            AppPermission.Location.manifestPermissions(sdk = 24),
        )
    }

    @Test
    fun `background location asks from Android 10 and is covered by the foreground grant below`() {
        assertEquals(
            listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            AppPermission.BackgroundLocation.manifestPermissions(sdk = 29),
        )
        assertTrue(AppPermission.BackgroundLocation.manifestPermissions(sdk = 28).isEmpty())
    }
}

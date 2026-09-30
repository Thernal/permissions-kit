package io.thernal.permissionskit.permissions.impl.presentation

import android.Manifest
import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AndroidPermissionStatusTest {
    @Test
    fun `granted wins over everything`() {
        assertEquals(
            PermissionStatus.Granted,
            androidPermissionStatus(granted = true, shouldShowRationale = true, requestedBefore = true),
        )
    }

    @Test
    fun `a rationale means the system asks again`() {
        assertEquals(
            PermissionStatus.ShouldShowRationale,
            androidPermissionStatus(granted = false, shouldShowRationale = true, requestedBefore = true),
        )
    }

    @Test
    fun `refused without a rationale after asking is denied for good`() {
        assertEquals(
            PermissionStatus.Denied,
            androidPermissionStatus(granted = false, shouldShowRationale = false, requestedBefore = true),
        )
    }

    @Test
    fun `never asked is not determined`() {
        assertEquals(
            PermissionStatus.NotDetermined,
            androidPermissionStatus(granted = false, shouldShowRationale = false, requestedBefore = false),
        )
    }

    @Test
    fun `notifications ask for a permission from Android 13`() {
        assertEquals(Manifest.permission.POST_NOTIFICATIONS, AppPermission.Notification.manifestPermission(sdk = 33))
        assertNull(AppPermission.Notification.manifestPermission(sdk = 32))
    }

    @Test
    fun `the photo library never asks, the camera and microphone always do`() {
        assertNull(AppPermission.PhotoLibrary.manifestPermission(sdk = 36))
        assertEquals(Manifest.permission.CAMERA, AppPermission.Camera.manifestPermission(sdk = 24))
        assertEquals(Manifest.permission.RECORD_AUDIO, AppPermission.Microphone.manifestPermission(sdk = 24))
    }
}

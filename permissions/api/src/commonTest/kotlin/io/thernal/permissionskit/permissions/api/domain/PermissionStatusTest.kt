package io.thernal.permissionskit.permissions.api.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class PermissionStatusTest {
    private val all = listOf(
        PermissionStatus.Granted,
        PermissionStatus.Limited,
        PermissionStatus.NotDetermined,
        PermissionStatus.ShouldShowRationale,
        PermissionStatus.Denied,
        PermissionStatus.Restricted,
    )

    @Test
    fun `granted and limited access let the feature run`() {
        assertEquals(listOf(PermissionStatus.Granted, PermissionStatus.Limited), all.filter { it.isGranted })
    }

    @Test
    fun `only a status the system still asks about can be requested`() {
        assertEquals(
            listOf(PermissionStatus.NotDetermined, PermissionStatus.ShouldShowRationale),
            all.filter { it.canRequest },
        )
    }
}

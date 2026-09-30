package io.thernal.permissionskit.permissions.impl.presentation

import io.thernal.permissionskit.permissions.api.domain.PermissionStatus
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import kotlin.test.Test
import kotlin.test.assertEquals

class IosStatusMappingTest {
    @Test
    fun `capture device statuses`() {
        assertEquals(PermissionStatus.Granted, avStatus(AVAuthorizationStatusAuthorized))
        assertEquals(PermissionStatus.Denied, avStatus(AVAuthorizationStatusDenied))
        assertEquals(PermissionStatus.Restricted, avStatus(AVAuthorizationStatusRestricted))
        assertEquals(PermissionStatus.NotDetermined, avStatus(AVAuthorizationStatusNotDetermined))
    }

    @Test
    fun `photo library statuses keep limited access apart`() {
        assertEquals(PermissionStatus.Granted, photoStatus(PHAuthorizationStatusAuthorized))
        assertEquals(PermissionStatus.Limited, photoStatus(PHAuthorizationStatusLimited))
        assertEquals(PermissionStatus.Denied, photoStatus(PHAuthorizationStatusDenied))
        assertEquals(PermissionStatus.Restricted, photoStatus(PHAuthorizationStatusRestricted))
        assertEquals(PermissionStatus.NotDetermined, photoStatus(PHAuthorizationStatusNotDetermined))
    }

    @Test
    fun `provisional and ephemeral notifications count as granted`() {
        assertEquals(PermissionStatus.Granted, notificationStatus(UNAuthorizationStatusAuthorized))
        assertEquals(PermissionStatus.Granted, notificationStatus(UNAuthorizationStatusProvisional))
        assertEquals(PermissionStatus.Granted, notificationStatus(UNAuthorizationStatusEphemeral))
        assertEquals(PermissionStatus.Denied, notificationStatus(UNAuthorizationStatusDenied))
        assertEquals(PermissionStatus.NotDetermined, notificationStatus(UNAuthorizationStatusNotDetermined))
    }
}

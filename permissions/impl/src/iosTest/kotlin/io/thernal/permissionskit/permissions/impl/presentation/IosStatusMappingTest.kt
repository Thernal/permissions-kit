package io.thernal.permissionskit.permissions.impl.presentation

import io.thernal.permissionskit.permissions.api.domain.PermissionStatus
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
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

    @Test
    fun `when in use grants the foreground location whatever was asked`() {
        assertEquals(
            PermissionStatus.Granted,
            locationStatus(status = kCLAuthorizationStatusAuthorizedWhenInUse, isAlways = false, isUpgradeAsked = true),
        )
    }

    @Test
    fun `when in use leaves always to ask until the upgrade prompt was used`() {
        assertEquals(
            PermissionStatus.NotDetermined,
            locationStatus(status = kCLAuthorizationStatusAuthorizedWhenInUse, isAlways = true, isUpgradeAsked = false),
        )
        assertEquals(
            PermissionStatus.Denied,
            locationStatus(status = kCLAuthorizationStatusAuthorizedWhenInUse, isAlways = true, isUpgradeAsked = true),
        )
    }

    @Test
    fun `the other location statuses mean the same for both permissions`() {
        listOf(true, false).forEach { isAlways ->
            assertEquals(
                PermissionStatus.Granted,
                locationStatus(
                    status = kCLAuthorizationStatusAuthorizedAlways,
                    isAlways = isAlways,
                    isUpgradeAsked = false,
                ),
            )
            assertEquals(
                PermissionStatus.Denied,
                locationStatus(status = kCLAuthorizationStatusDenied, isAlways = isAlways, isUpgradeAsked = false),
            )
            assertEquals(
                PermissionStatus.Restricted,
                locationStatus(status = kCLAuthorizationStatusRestricted, isAlways = isAlways, isUpgradeAsked = false),
            )
            assertEquals(
                PermissionStatus.NotDetermined,
                locationStatus(
                    status = kCLAuthorizationStatusNotDetermined,
                    isAlways = isAlways,
                    isUpgradeAsked = false,
                ),
            )
        }
    }
}

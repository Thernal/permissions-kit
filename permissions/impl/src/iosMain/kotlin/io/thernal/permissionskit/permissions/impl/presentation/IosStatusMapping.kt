package io.thernal.permissionskit.permissions.impl.presentation

import io.thernal.permissionskit.permissions.api.domain.PermissionStatus
import platform.AVFoundation.AVAuthorizationStatus
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.Photos.PHAuthorizationStatus
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusRestricted
import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional

internal fun avStatus(status: AVAuthorizationStatus): PermissionStatus {
    return when (status) {
        AVAuthorizationStatusAuthorized -> PermissionStatus.Granted
        AVAuthorizationStatusDenied -> PermissionStatus.Denied
        AVAuthorizationStatusRestricted -> PermissionStatus.Restricted
        else -> PermissionStatus.NotDetermined
    }
}

internal fun photoStatus(status: PHAuthorizationStatus): PermissionStatus {
    return when (status) {
        PHAuthorizationStatusAuthorized -> PermissionStatus.Granted
        PHAuthorizationStatusLimited -> PermissionStatus.Limited
        PHAuthorizationStatusDenied -> PermissionStatus.Denied
        PHAuthorizationStatusRestricted -> PermissionStatus.Restricted
        else -> PermissionStatus.NotDetermined
    }
}

/** Provisional and ephemeral authorisations deliver notifications, so they count as granted. */
internal fun notificationStatus(status: UNAuthorizationStatus): PermissionStatus {
    return when (status) {
        UNAuthorizationStatusAuthorized,
        UNAuthorizationStatusProvisional,
        UNAuthorizationStatusEphemeral,
        -> PermissionStatus.Granted

        UNAuthorizationStatusDenied -> PermissionStatus.Denied

        else -> PermissionStatus.NotDetermined
    }
}

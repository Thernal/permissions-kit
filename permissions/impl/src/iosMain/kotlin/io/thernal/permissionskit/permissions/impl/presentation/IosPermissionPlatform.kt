package io.thernal.permissionskit.permissions.impl.presentation

import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus
import io.thernal.permissionskit.permissions.impl.domain.PermissionPlatform
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.Photos.PHAccessLevelReadWrite
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * The iOS frameworks answer "never asked" themselves, so nothing is recorded here. Their completion
 * handlers run on background queues; resuming the continuation returns to the caller's dispatcher.
 */
internal object IosPermissionPlatform : PermissionPlatform {
    private val NOTIFICATION_OPTIONS =
        UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound

    // The notification status is only available asynchronously; this is the last one read.
    private var lastNotificationStatus: PermissionStatus = PermissionStatus.NotDetermined

    override fun peek(permission: AppPermission): PermissionStatus {
        return when (permission) {
            AppPermission.Camera -> avStatus(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo))

            AppPermission.Microphone -> avStatus(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeAudio))

            AppPermission.PhotoLibrary -> photoStatus(
                PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite),
            )

            AppPermission.Notification -> lastNotificationStatus
        }
    }

    override suspend fun status(permission: AppPermission): PermissionStatus {
        if (permission != AppPermission.Notification) {
            return peek(permission)
        }
        val status = suspendCoroutine { continuation ->
            val center = UNUserNotificationCenter.currentNotificationCenter()
            center.getNotificationSettingsWithCompletionHandler { settings ->
                continuation.resume(settings?.let { notificationStatus(it.authorizationStatus) })
            }
        } ?: lastNotificationStatus
        lastNotificationStatus = status
        return status
    }

    /** One prompt after another: iOS shows a single system alert at a time. */
    override suspend fun request(permissions: List<AppPermission>) {
        permissions
            .filter { status(it) == PermissionStatus.NotDetermined }
            .forEach { requestOne(it) }
    }

    override fun openSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }

    private suspend fun requestOne(permission: AppPermission) {
        suspendCoroutine { continuation ->
            when (permission) {
                AppPermission.Camera -> AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { _ ->
                    continuation.resume(Unit)
                }

                AppPermission.Microphone -> AVCaptureDevice.requestAccessForMediaType(AVMediaTypeAudio) { _ ->
                    continuation.resume(Unit)
                }

                AppPermission.PhotoLibrary -> PHPhotoLibrary.requestAuthorizationForAccessLevel(
                    PHAccessLevelReadWrite,
                ) { _ ->
                    continuation.resume(Unit)
                }

                AppPermission.Notification -> UNUserNotificationCenter.currentNotificationCenter()
                    .requestAuthorizationWithOptions(NOTIFICATION_OPTIONS) { _, _ ->
                        continuation.resume(Unit)
                    }
            }
        }
    }
}

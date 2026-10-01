package io.thernal.permissionskit.permissions.impl.presentation

import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus
import io.thernal.permissionskit.permissions.impl.domain.PermissionPlatform
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
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

    // Created on first use — from the composition, so on the main thread, as CLLocationManager needs.
    private val location by lazy { LocationAuthorization() }

    override fun peek(permission: AppPermission): PermissionStatus {
        return when (permission) {
            AppPermission.Camera -> avStatus(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo))

            AppPermission.Microphone -> avStatus(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeAudio))

            AppPermission.PhotoLibrary -> photoStatus(
                PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite),
            )

            AppPermission.Notification -> lastNotificationStatus

            AppPermission.Location -> locationStatus(
                status = location.status,
                isAlways = false,
                isUpgradeAsked = location.isUpgradeAsked,
            )

            AppPermission.BackgroundLocation -> locationStatus(
                status = location.status,
                isAlways = true,
                isUpgradeAsked = location.isUpgradeAsked,
            )
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

    override fun openSettings(permissions: List<AppPermission>) {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }

    private suspend fun requestOne(permission: AppPermission) {
        when (permission) {
            AppPermission.Camera -> awaitAnswer { done ->
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { _ -> done() }
            }

            AppPermission.Microphone -> awaitAnswer { done ->
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeAudio) { _ -> done() }
            }

            AppPermission.PhotoLibrary -> awaitAnswer { done ->
                PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelReadWrite) { _ -> done() }
            }

            AppPermission.Notification -> awaitAnswer { done ->
                UNUserNotificationCenter.currentNotificationCenter()
                    .requestAuthorizationWithOptions(NOTIFICATION_OPTIONS) { _, _ -> done() }
            }

            AppPermission.Location -> location.requestWhenInUse()

            // Always is an upgrade of When In Use: asked from nothing, that comes first.
            AppPermission.BackgroundLocation -> {
                if (location.status == kCLAuthorizationStatusNotDetermined) {
                    location.requestWhenInUse()
                }
                if (location.status == kCLAuthorizationStatusAuthorizedWhenInUse) {
                    location.requestAlways()
                }
            }
        }
    }

    private suspend fun awaitAnswer(ask: (done: () -> Unit) -> Unit) {
        suspendCoroutine { continuation -> ask { continuation.resume(Unit) } }
    }
}

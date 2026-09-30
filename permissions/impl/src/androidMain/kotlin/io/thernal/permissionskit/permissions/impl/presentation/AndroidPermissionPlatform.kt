package io.thernal.permissionskit.permissions.impl.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus
import io.thernal.permissionskit.permissions.impl.domain.PermissionPlatform

internal class AndroidPermissionPlatform(
    private val context: Context,
    private val activity: Activity?,
    private val ledger: RequestLedger,
    private val launch: suspend (Array<String>) -> Unit,
) : PermissionPlatform {
    override fun peek(permission: AppPermission): PermissionStatus {
        val manifestPermission = permission.manifestPermission()
        return when {
            manifestPermission != null -> androidPermissionStatus(
                granted = isGranted(manifestPermission),
                shouldShowRationale = activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(it, manifestPermission)
                } == true,
                requestedBefore = ledger.wasRequested(manifestPermission),
            )

            // Below Android 13 notifications need no permission, but the user can still turn them off.
            permission == AppPermission.Notification -> if (notificationsEnabled()) {
                PermissionStatus.Granted
            } else {
                PermissionStatus.Denied
            }

            else -> PermissionStatus.Granted
        }
    }

    override suspend fun status(permission: AppPermission): PermissionStatus {
        return peek(permission)
    }

    override suspend fun request(permissions: List<AppPermission>) {
        // A permission refused for good is launched too: the system answers at once without a
        // dialog, and a dialog the user dismissed without answering is shown again.
        val names = permissions
            .mapNotNull { it.manifestPermission() }
            .filterNot(::isGranted)
        if (names.isEmpty()) {
            return
        }
        ledger.markRequested(names)
        launch(names.toTypedArray())
    }

    override fun openSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun isGranted(manifestPermission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, manifestPermission) == PackageManager.PERMISSION_GRANTED
    }

    private fun notificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

/** The manifest permission behind [this], or `null` where this Android version asks for none. */
internal fun AppPermission.manifestPermission(sdk: Int = Build.VERSION.SDK_INT): String? {
    return when (this) {
        AppPermission.Camera -> Manifest.permission.CAMERA

        AppPermission.Microphone -> Manifest.permission.RECORD_AUDIO

        AppPermission.Notification -> if (sdk >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }

        // The system Photo Picker needs no permission, and Play restricts READ_MEDIA_*.
        AppPermission.PhotoLibrary -> null
    }
}

/**
 * Android answers "granted" and "show a rationale"; "refused for good" and "never asked" look the
 * same — both no and no — so whether this app asked before decides between them.
 */
internal fun androidPermissionStatus(
    granted: Boolean,
    shouldShowRationale: Boolean,
    requestedBefore: Boolean,
): PermissionStatus {
    return when {
        granted -> PermissionStatus.Granted
        shouldShowRationale -> PermissionStatus.ShouldShowRationale
        requestedBefore -> PermissionStatus.Denied
        else -> PermissionStatus.NotDetermined
    }
}

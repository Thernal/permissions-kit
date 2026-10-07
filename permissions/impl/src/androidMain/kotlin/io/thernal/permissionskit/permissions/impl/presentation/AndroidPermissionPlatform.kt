package io.thernal.permissionskit.permissions.impl.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.time.TimeSource
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
        // Android asks for "all the time" only on top of the foreground grant: until that is in, it is the
        // step to take, and below Android 10 it is the whole of it.
        if (permission == AppPermission.BackgroundLocation && !isGranted(AppPermission.Location)) {
            return peek(AppPermission.Location)
        }
        val names = permission.manifestPermissions()
        return when {
            names.isNotEmpty() -> androidPermissionStatus(
                granted = names.any(::isGranted),
                shouldShowRationale = names.any(::shouldShowRationale),
                isRefused = names.all(ledger::isRefused),
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

    /**
     * Two rounds: background location is asked only once the foreground grant is in, and Android 11+
     * refuses it when asked together with anything else.
     */
    override suspend fun request(permissions: List<AppPermission>) {
        val firstRound = permissions.flatMap { permission ->
            if (permission == AppPermission.BackgroundLocation) {
                AppPermission.Location.manifestPermissions()
            } else {
                permission.manifestPermissions()
            }
        }
        launchRound(firstRound.distinct().filterNot(::isGranted))
        if (AppPermission.BackgroundLocation in permissions && isGranted(AppPermission.Location)) {
            launchRound(AppPermission.BackgroundLocation.manifestPermissions().filterNot(::isGranted))
        }
    }

    /**
     * One system dialog for [names]. A permission refused for good is launched too: the system answers at
     * once, without a dialog — which is how that is told apart from a dialog the user dismissed.
     */
    private suspend fun launchRound(names: List<String>) {
        if (names.isEmpty()) {
            return
        }
        val before = names.associateWith(::nameStatus)
        val isDialogShown = pausesDuring { launch(names.toTypedArray()) }
        ledger.setRefused(
            names.associateWith { name ->
                isRefusedAfterRequest(
                    granted = isGranted(name),
                    shouldShowRationale = shouldShowRationale(name),
                    isDialogShown = isDialogShown,
                    before = before.getValue(name),
                )
            },
        )
    }

    /**
     * Runs [block] and says whether the system showed its dialog: the dialog is an activity of its
     * own, so the host pauses — but it also pauses, briefly, when the system answers by itself, so
     * the pause must last (see [isDialogShown]). Without a lifecycle to watch, a dialog is assumed.
     */
    private suspend fun pausesDuring(block: suspend () -> Unit): Boolean {
        val lifecycle = (activity as? LifecycleOwner)?.lifecycle
        var hasPaused = lifecycle == null
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                hasPaused = true
            }
        }
        lifecycle?.addObserver(observer)
        val started = TimeSource.Monotonic.markNow()
        try {
            block()
        } finally {
            lifecycle?.removeObserver(observer)
        }
        return isDialogShown(hasPaused = hasPaused, elapsed = started.elapsedNow())
    }

    /**
     * The user may set a refused permission back to "Ask every time" there: forget the refusal. If it
     * still stands, the next request comes back without a dialog and records it again.
     */
    override fun openSettings(permissions: List<AppPermission>) {
        val names = permissions.flatMap { permission ->
            if (permission == AppPermission.BackgroundLocation) {
                AppPermission.Location.manifestPermissions() + permission.manifestPermissions()
            } else {
                permission.manifestPermissions()
            }
        }
        ledger.setRefused(names.associateWith { false })
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun nameStatus(manifestPermission: String): PermissionStatus {
        return androidPermissionStatus(
            granted = isGranted(manifestPermission),
            shouldShowRationale = shouldShowRationale(manifestPermission),
            isRefused = ledger.isRefused(manifestPermission),
        )
    }

    /** Location counts as granted with either accuracy: approximate is the user's choice to make. */
    private fun isGranted(permission: AppPermission): Boolean {
        return permission.manifestPermissions().any(::isGranted)
    }

    private fun shouldShowRationale(manifestPermission: String): Boolean {
        return activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, manifestPermission) } == true
    }

    private fun isGranted(manifestPermission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, manifestPermission) == PackageManager.PERMISSION_GRANTED
    }

    private fun notificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

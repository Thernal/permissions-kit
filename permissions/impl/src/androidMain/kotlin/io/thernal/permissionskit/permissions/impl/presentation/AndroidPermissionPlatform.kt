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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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

/**
 * The manifest permissions behind [this] — one dialog asks for all of them — or none where this Android
 * version asks for nothing.
 */
internal fun AppPermission.manifestPermissions(sdk: Int = Build.VERSION.SDK_INT): List<String> {
    return when (this) {
        AppPermission.Camera -> listOf(Manifest.permission.CAMERA)

        AppPermission.Microphone -> listOf(Manifest.permission.RECORD_AUDIO)

        AppPermission.Notification -> if (sdk >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList()
        }

        // The system Photo Picker needs no permission, and Play restricts READ_MEDIA_*.
        AppPermission.PhotoLibrary -> emptyList()

        // Both, so the dialog offers the choice between precise and approximate.
        AppPermission.Location -> listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        // Below Android 10 the foreground grant covers the background.
        AppPermission.BackgroundLocation -> if (sdk >= Build.VERSION_CODES.Q) {
            listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            emptyList()
        }
    }
}

/**
 * Android answers "granted" and "show a rationale". "Refused for good" looks like "never asked", a
 * lapsed one-time grant and "Ask every time" — none granted, no rationale — so the ledger's record of
 * a refusal decides; everything else can still be asked.
 */
internal fun androidPermissionStatus(
    granted: Boolean,
    shouldShowRationale: Boolean,
    isRefused: Boolean,
): PermissionStatus {
    return when {
        granted -> PermissionStatus.Granted
        shouldShowRationale -> PermissionStatus.ShouldShowRationale
        isRefused -> PermissionStatus.Denied
        else -> PermissionStatus.NotDetermined
    }
}

/** Answered without a dialog, the system is back in about a tenth of a second; no person answers that fast. */
private val FASTEST_ANSWER = 300.milliseconds

/**
 * Whether a request showed the system dialog: the host paused, and for longer than the system takes
 * to answer by itself (measured ~115 ms on Android 15) — a person needs well over [FASTEST_ANSWER].
 */
internal fun isDialogShown(
    hasPaused: Boolean,
    elapsed: Duration,
): Boolean {
    return hasPaused && elapsed >= FASTEST_ANSWER
}

/**
 * Whether the system stopped asking, from how a request ended. No dialog at all means it answered by
 * itself: refused for good. A refusal in the dialog without a rationale afterwards is final only when
 * the user had refused before (Android's second refusal); after a first dialog it was a dismissal.
 */
internal fun isRefusedAfterRequest(
    granted: Boolean,
    shouldShowRationale: Boolean,
    isDialogShown: Boolean,
    before: PermissionStatus,
): Boolean {
    return when {
        granted || shouldShowRationale -> false
        !isDialogShown -> true
        else -> before == PermissionStatus.ShouldShowRationale || before == PermissionStatus.Denied
    }
}

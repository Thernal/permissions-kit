package io.thernal.permissionskit.permissions.impl.presentation

import io.thernal.permissionskit.permissions.api.domain.PermissionStatus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

/** Answered without a dialog, the system is back in about a tenth of a second; no person answers that fast. */
private val FASTEST_ANSWER = 300.milliseconds

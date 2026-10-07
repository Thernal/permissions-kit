package io.thernal.permissionskit.permissions.api.domain

import androidx.compose.runtime.Immutable

/** Where a permission stands now. Read again whenever the app comes back to the foreground. */
@Immutable
sealed interface PermissionStatus {
    /** Allowed. */
    data object Granted : PermissionStatus

    /** iOS only: allowed for part of the data — the photos the user picked. */
    data object Limited : PermissionStatus

    /** Never asked: `request()` shows the system prompt. */
    data object NotDetermined : PermissionStatus

    /**
     * Android only: refused once, and the system will ask again. Explain why the feature needs it
     * before calling `request()`.
     */
    data object ShouldShowRationale : PermissionStatus

    /** Refused, and the system will not ask again: only the app's settings can change it (`openSettings()`). */
    data object Denied : PermissionStatus

    /** iOS only: blocked by a device policy (parental controls, MDM). Neither the app nor the user can change it. */
    data object Restricted : PermissionStatus
}

package io.thernal.permissionskit.permissions.api.presentation

import androidx.compose.runtime.Stable
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus

/** One permission as a composable sees it. The status changes as the user answers or visits settings. */
@Stable
interface PermissionState {
    val status: PermissionStatus

    /** Shows the system prompt when it still can; otherwise only re-reads the status. */
    val request: () -> Unit

    /** Opens this app's page in the system settings — the way out of [PermissionStatus.Denied]. */
    val openSettings: () -> Unit
}

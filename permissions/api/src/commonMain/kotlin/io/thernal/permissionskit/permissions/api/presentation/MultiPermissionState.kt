package io.thernal.permissionskit.permissions.api.presentation

import androidx.compose.runtime.Stable
import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus
import io.thernal.permissionskit.permissions.api.domain.isGranted

/** Several permissions asked together — one system dialog on Android, one prompt after another on iOS. */
@Stable
interface MultiPermissionState {
    /** One entry per requested permission, in the order they were passed. */
    val statuses: Map<AppPermission, PermissionStatus>

    /** Prompts for every permission that is not granted and can still be asked. */
    val request: () -> Unit

    val openSettings: () -> Unit

    val areAllGranted: Boolean
        get() = statuses.values.all { it.isGranted }
}

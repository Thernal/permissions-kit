package io.thernal.permissionskit.permissions.api.presentation

import androidx.compose.runtime.Composable
import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus

/** Several permissions together; call `request()` on the result to prompt. */
@Composable
fun rememberPermissionState(permissions: List<AppPermission>): MultiPermissionState {
    return LocalPermissionStateProvider.current.rememberPermissionState(permissions)
}

/** One permission; call `request()` on the result to prompt. */
@Composable
fun rememberPermissionState(permission: AppPermission): PermissionState {
    val multi = LocalPermissionStateProvider.current.rememberPermissionState(listOf(permission))
    return SinglePermissionState(
        status = multi.statuses[permission] ?: PermissionStatus.NotDetermined,
        request = multi.request,
        openSettings = multi.openSettings,
    )
}

private class SinglePermissionState(
    override val status: PermissionStatus,
    override val request: () -> Unit,
    override val openSettings: () -> Unit,
) : PermissionState

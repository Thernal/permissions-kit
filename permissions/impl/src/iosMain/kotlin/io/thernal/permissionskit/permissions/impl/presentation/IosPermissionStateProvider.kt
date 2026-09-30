package io.thernal.permissionskit.permissions.impl.presentation

import androidx.compose.runtime.Composable
import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.presentation.MultiPermissionState
import io.thernal.permissionskit.permissions.api.presentation.PermissionStateProvider

actual fun platformPermissionStateProvider(): PermissionStateProvider {
    return IosPermissionStateProvider
}

internal object IosPermissionStateProvider : PermissionStateProvider {
    @Composable
    override fun rememberPermissionState(permissions: List<AppPermission>): MultiPermissionState {
        return rememberPermissionStates(platform = IosPermissionPlatform, permissions = permissions)
    }
}

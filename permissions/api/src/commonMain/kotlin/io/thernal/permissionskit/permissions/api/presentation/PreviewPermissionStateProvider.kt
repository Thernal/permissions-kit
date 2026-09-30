package io.thernal.permissionskit.permissions.api.presentation

import androidx.compose.runtime.Composable
import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus

/** Every permission granted, requests and settings ignored: an isolated preview never prompts. */
internal object PreviewPermissionStateProvider : PermissionStateProvider {
    @Composable
    override fun rememberPermissionState(permissions: List<AppPermission>): MultiPermissionState {
        return object : MultiPermissionState {
            override val statuses = permissions.associateWith { PermissionStatus.Granted }
            override val request: () -> Unit = {}
            override val openSettings: () -> Unit = {}
        }
    }
}

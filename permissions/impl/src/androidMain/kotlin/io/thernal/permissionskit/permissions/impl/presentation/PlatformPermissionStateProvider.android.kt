package io.thernal.permissionskit.permissions.impl.presentation

import io.thernal.permissionskit.permissions.api.presentation.PermissionStateProvider

actual fun platformPermissionStateProvider(): PermissionStateProvider {
    return AndroidPermissionStateProvider
}

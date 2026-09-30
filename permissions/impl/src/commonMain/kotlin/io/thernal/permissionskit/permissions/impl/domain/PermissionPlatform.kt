package io.thernal.permissionskit.permissions.impl.domain

import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus

/** What one platform can tell and do about permissions. Everything above it is shared. */
internal interface PermissionPlatform {
    /**
     * The status without waiting: exact where the platform answers synchronously, otherwise the last
     * one read ([PermissionStatus.NotDetermined] before any) until [status] replaces it.
     */
    fun peek(permission: AppPermission): PermissionStatus

    /** The status as the platform reports it now. */
    suspend fun status(permission: AppPermission): PermissionStatus

    /**
     * Shows the system prompts for [permissions] — every one not granted — and returns once they are
     * answered. A platform skips those it can no longer ask for.
     */
    suspend fun request(permissions: List<AppPermission>)

    fun openSettings()
}

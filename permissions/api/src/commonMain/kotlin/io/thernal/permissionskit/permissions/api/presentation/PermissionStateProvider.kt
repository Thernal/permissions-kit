package io.thernal.permissionskit.permissions.api.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import io.thernal.permissionskit.permissions.api.domain.AppPermission

/**
 * Reads and requests runtime permissions. `impl` holds the platform code — the Android launcher and
 * manifest lookups, the iOS framework calls — so a feature compiles against this contract only;
 * `wiring` installs the real one as [LocalPermissionStateProvider].
 */
interface PermissionStateProvider {
    @Composable
    fun rememberPermissionState(permissions: List<AppPermission>): MultiPermissionState
}

/**
 * The provider the composables below use. Without `wiring` — an isolated preview — it reports
 * every permission granted and never prompts.
 */
val LocalPermissionStateProvider = staticCompositionLocalOf<PermissionStateProvider> {
    PreviewPermissionStateProvider
}

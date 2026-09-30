package io.thernal.permissionskit.permissions.api.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus

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

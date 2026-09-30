package io.thernal.permissionskit.permissions.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus
import io.thernal.permissionskit.permissions.api.domain.isGranted
import io.thernal.permissionskit.permissions.api.presentation.MultiPermissionState
import io.thernal.permissionskit.permissions.api.presentation.PermissionStateProvider

/**
 * A provider a test or preview drives: statuses are set with [set], every `request()` is recorded in
 * [requests] and answered by [answer], every `openSettings()` counted in [settingsOpened].
 *
 * ```
 * val permissions = FakePermissionStateProvider(AppPermission.Camera to PermissionStatus.Denied)
 * CompositionLocalProvider(LocalPermissionStateProvider provides permissions) { CameraScreen() }
 * ```
 *
 * A permission never set is [PermissionStatus.NotDetermined]. Statuses are snapshot state, so a
 * composition reading them recomposes when a test changes one.
 */
class FakePermissionStateProvider(
    vararg initial: Pair<AppPermission, PermissionStatus>,
    /** What a request turns each asked permission into; by default the user allows everything. */
    var answer: (AppPermission) -> PermissionStatus = { PermissionStatus.Granted },
) : PermissionStateProvider {
    private val statuses = mutableStateMapOf(*initial)
    private val recordedRequests = mutableListOf<List<AppPermission>>()

    /** The permissions each `request()` asked for, oldest first. */
    val requests: List<List<AppPermission>>
        get() = recordedRequests.toList()

    var settingsOpened: Int = 0
        private set

    fun status(permission: AppPermission): PermissionStatus {
        return statuses[permission] ?: PermissionStatus.NotDetermined
    }

    fun set(
        permission: AppPermission,
        status: PermissionStatus,
    ) {
        statuses[permission] = status
    }

    /** The same state `rememberPermissionState(permissions)` returns, for a test without a composition. */
    fun stateFor(permissions: List<AppPermission>): MultiPermissionState {
        return FakeMultiPermissionState(permissions)
    }

    @Composable
    override fun rememberPermissionState(permissions: List<AppPermission>): MultiPermissionState {
        return remember(permissions) { stateFor(permissions) }
    }

    private inner class FakeMultiPermissionState(
        private val permissions: List<AppPermission>,
    ) : MultiPermissionState {
        override val statuses: Map<AppPermission, PermissionStatus>
            get() = permissions.associateWith(::status)

        override val request: () -> Unit = {
            recordedRequests += permissions
            permissions
                .filter { !status(it).isGranted && status(it) != PermissionStatus.Restricted }
                .forEach { set(permission = it, status = answer(it)) }
        }

        override val openSettings: () -> Unit = { settingsOpened++ }
    }
}

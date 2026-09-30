package io.thernal.permissionskit.permissions.impl.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus
import io.thernal.permissionskit.permissions.api.presentation.MultiPermissionState
import io.thernal.permissionskit.permissions.impl.domain.PermissionPlatform
import io.thernal.permissionskit.permissions.impl.domain.PermissionStatesController
import kotlinx.coroutines.launch

/**
 * The shared half of both providers: statuses re-read on every resume (the user may come back from
 * settings), requests run in the composition's scope.
 */
@Composable
internal fun rememberPermissionStates(
    platform: PermissionPlatform,
    permissions: List<AppPermission>,
): MultiPermissionState {
    val controller = remember(key1 = platform, key2 = permissions) {
        PermissionStatesController(platform = platform, permissions = permissions)
    }
    val scope = rememberCoroutineScope()
    val statuses = controller.statuses.collectAsState()

    // A lifecycle that is already resumed replays ON_RESUME to a new observer, so this is also the
    // first read.
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        scope.launch { controller.refresh() }
    }

    return remember(key1 = controller, key2 = scope) {
        ControllerPermissionState(
            statusesState = statuses,
            request = { scope.launch { controller.request() } },
            openSettings = controller::openSettings,
        )
    }
}

private class ControllerPermissionState(
    private val statusesState: State<Map<AppPermission, PermissionStatus>>,
    override val request: () -> Unit,
    override val openSettings: () -> Unit,
) : MultiPermissionState {
    override val statuses: Map<AppPermission, PermissionStatus>
        get() = statusesState.value
}

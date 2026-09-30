package io.thernal.permissionskit.permissions.impl.domain

import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus
import io.thernal.permissionskit.permissions.api.domain.isGranted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

/**
 * The statuses of one set of permissions and the one request that may be in flight for them. A
 * second `request()` while the prompts are showing does nothing: the platform would queue or drop it.
 */
internal class PermissionStatesController(
    private val platform: PermissionPlatform,
    private val permissions: List<AppPermission>,
) {
    private val mutableStatuses = MutableStateFlow(permissions.associateWith(platform::peek))
    private val requesting = Mutex()

    val statuses: StateFlow<Map<AppPermission, PermissionStatus>> = mutableStatuses.asStateFlow()

    suspend fun refresh() {
        mutableStatuses.value = permissions.associateWith { platform.status(it) }
    }

    /** Re-reads first — the user may have changed a permission in settings — then prompts for what is missing. */
    suspend fun request() {
        if (!requesting.tryLock()) {
            return
        }
        try {
            refresh()
            val missing = permissions.filter { permission ->
                val status = mutableStatuses.value.getValue(permission)
                !status.isGranted && status != PermissionStatus.Restricted
            }
            if (missing.isNotEmpty()) {
                platform.request(missing)
                refresh()
            }
        } finally {
            requesting.unlock()
        }
    }

    fun openSettings() {
        platform.openSettings(permissions)
    }
}

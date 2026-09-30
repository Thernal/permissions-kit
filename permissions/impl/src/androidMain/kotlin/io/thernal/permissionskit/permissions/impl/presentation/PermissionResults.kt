package io.thernal.permissionskit.permissions.impl.presentation

import kotlinx.coroutines.CompletableDeferred

/**
 * Turns the launcher's callback into a suspension. When the activity is recreated while the dialog
 * shows, the old composition's scope is cancelled with the wait; the new one reads the answer on resume.
 */
internal class PermissionResults {
    private var pending: CompletableDeferred<Unit>? = null

    suspend fun await(launch: () -> Unit) {
        val result = CompletableDeferred<Unit>()
        pending = result
        launch()
        result.await()
    }

    fun onResult() {
        pending?.complete(Unit)
        pending = null
    }
}

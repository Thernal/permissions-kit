package io.thernal.permissionskit.permissions.impl.presentation

import android.content.Context

/**
 * Which manifest permissions the system has stopped asking for ("don't ask again"). Android reports
 * that exactly like "never asked" — not granted, no rationale — and also like a one-time grant that
 * has lapsed or a permission set to "Ask every time"; only what happened on the last request tells
 * them apart, so it is recorded here.
 */
internal class RequestLedger(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun isRefused(permission: String): Boolean {
        return preferences.getBoolean(permission, false)
    }

    fun setRefused(refused: Map<String, Boolean>) {
        val editor = preferences.edit()
        refused.forEach { (permission, isRefused) ->
            if (isRefused) {
                editor.putBoolean(permission, true)
            } else {
                editor.remove(permission)
            }
        }
        editor.apply()
    }

    private companion object {
        // A new file: the earlier "requested" record meant something else and must not be read as this.
        const val FILE_NAME = "io.thernal.permissionskit.permissions.refused"
    }
}

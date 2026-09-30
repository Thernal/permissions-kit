package io.thernal.permissionskit.permissions.impl.presentation

import android.content.Context

/**
 * Which manifest permissions this app has asked for, ever. Android reports a refused permission
 * that will not be asked again exactly like one never asked; only this record tells them apart.
 */
internal class RequestLedger(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun wasRequested(permission: String): Boolean {
        return preferences.getBoolean(permission, false)
    }

    fun markRequested(permissions: Collection<String>) {
        val editor = preferences.edit()
        permissions.forEach { editor.putBoolean(it, true) }
        editor.apply()
    }

    private companion object {
        const val FILE_NAME = "io.thernal.permissionskit.permissions.requested"
    }
}

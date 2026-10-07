package io.thernal.permissionskit.permissions.impl.presentation

import android.Manifest
import android.os.Build
import io.thernal.permissionskit.permissions.api.domain.AppPermission

/**
 * The manifest permissions behind [this] — one dialog asks for all of them — or none where this Android
 * version asks for nothing.
 */
internal fun AppPermission.manifestPermissions(sdk: Int = Build.VERSION.SDK_INT): List<String> {
    return when (this) {
        AppPermission.Camera -> listOf(Manifest.permission.CAMERA)

        AppPermission.Microphone -> listOf(Manifest.permission.RECORD_AUDIO)

        AppPermission.Notification -> if (sdk >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList()
        }

        // The system Photo Picker needs no permission, and Play restricts READ_MEDIA_*.
        AppPermission.PhotoLibrary -> emptyList()

        // Both, so the dialog offers the choice between precise and approximate.
        AppPermission.Location -> listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        // Below Android 10 the foreground grant covers the background.
        AppPermission.BackgroundLocation -> if (sdk >= Build.VERSION_CODES.Q) {
            listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            emptyList()
        }
    }
}

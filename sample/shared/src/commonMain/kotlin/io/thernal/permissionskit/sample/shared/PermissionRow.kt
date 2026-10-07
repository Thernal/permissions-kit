package io.thernal.permissionskit.sample.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus
import io.thernal.permissionskit.permissions.api.domain.canRequest
import io.thernal.permissionskit.permissions.api.domain.isGranted
import io.thernal.permissionskit.permissions.api.presentation.PermissionState

/** One permission: its name, its status, and the button that moves it on. */
@Composable
internal fun PermissionRow(
    permission: AppPermission,
    state: PermissionState,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(text = permission.name, style = MaterialTheme.typography.titleMedium)
            Text(text = state.status.label(), style = MaterialTheme.typography.bodySmall)
        }
        when {
            state.status.isGranted -> Unit

            state.status.canRequest -> Button(onClick = state.request) { Text(text = "Request") }

            state.status == PermissionStatus.Denied -> OutlinedButton(onClick = state.openSettings) {
                Text(text = "Settings")
            }

            else -> Unit
        }
    }
}

private fun PermissionStatus.label(): String {
    return when (this) {
        PermissionStatus.Granted -> "Granted"
        PermissionStatus.Limited -> "Limited — some photos"
        PermissionStatus.NotDetermined -> "Not asked yet"
        PermissionStatus.ShouldShowRationale -> "Refused once — explain, then ask again"
        PermissionStatus.Denied -> "Denied — only settings can change it"
        PermissionStatus.Restricted -> "Restricted by the device"
    }
}

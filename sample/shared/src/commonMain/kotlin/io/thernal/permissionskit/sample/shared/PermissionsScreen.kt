package io.thernal.permissionskit.sample.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.presentation.rememberPermissionState

/** Every permission the kit knows, each with its status and the action it allows. */
@Composable
internal fun PermissionsScreen() {
    val all = rememberPermissionState(AppPermission.entries)
    Column(
        modifier = Modifier
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "permissions-kit", style = MaterialTheme.typography.headlineSmall)
        AppPermission.entries.forEach { permission ->
            PermissionRow(permission = permission, state = rememberPermissionState(permission))
            HorizontalDivider()
        }
        Button(onClick = all.request, enabled = !all.areAllGranted) {
            val label = if (all.areAllGranted) {
                "Everything granted"
            } else {
                "Request all at once"
            }
            Text(text = label)
        }
    }
}

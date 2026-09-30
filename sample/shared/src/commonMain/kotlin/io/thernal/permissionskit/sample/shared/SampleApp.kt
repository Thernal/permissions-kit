package io.thernal.permissionskit.sample.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.zacsweers.metro.createGraph
import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus
import io.thernal.permissionskit.permissions.api.domain.canRequest
import io.thernal.permissionskit.permissions.api.domain.isGranted
import io.thernal.permissionskit.permissions.api.presentation.PermissionState
import io.thernal.permissionskit.permissions.api.presentation.rememberPermissionState

/** The sample's root: installs the graph's composition locals, as an application's root does. */
@Composable
fun SampleApp() {
    val graph = remember { createGraph<SampleGraph>() }
    // Once, at the root: the copy the spread makes is not worth avoiding.
    @Suppress("SpreadOperator")
    CompositionLocalProvider(*graph.compositionLocals.toTypedArray()) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                PermissionsScreen()
            }
        }
    }
}

@Composable
private fun PermissionsScreen() {
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

@Composable
private fun PermissionRow(
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

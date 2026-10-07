package io.thernal.permissionskit.permissions.impl.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.presentation.MultiPermissionState
import io.thernal.permissionskit.permissions.api.presentation.PermissionStateProvider

/** One `RequestMultiplePermissions` launcher per call site: every missing permission in one system dialog. */
internal object AndroidPermissionStateProvider : PermissionStateProvider {
    @Composable
    override fun rememberPermissionState(permissions: List<AppPermission>): MultiPermissionState {
        val context = LocalContext.current
        val results = remember { PermissionResults() }
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = { results.onResult() },
        )
        val platform = remember(key1 = context, key2 = launcher) {
            AndroidPermissionPlatform(
                context = context,
                activity = context.findActivity(),
                ledger = RequestLedger(context),
                launch = { names -> results.await { launcher.launch(names) } },
            )
        }
        return rememberPermissionStates(platform = platform, permissions = permissions)
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

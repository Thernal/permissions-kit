package io.thernal.permissionskit.permissions.impl.presentation

import io.thernal.permissionskit.permissions.api.presentation.PermissionStateProvider

/** The provider for the platform this is compiled for; `wiring` installs it. */
expect fun platformPermissionStateProvider(): PermissionStateProvider

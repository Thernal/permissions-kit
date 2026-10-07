package io.thernal.permissionskit.permissions.api.domain

/** The feature may run: [PermissionStatus.Granted], or [PermissionStatus.Limited] access. */
val PermissionStatus.isGranted: Boolean
    get() = this == PermissionStatus.Granted || this == PermissionStatus.Limited

/** `request()` can still show a system prompt. */
val PermissionStatus.canRequest: Boolean
    get() = this == PermissionStatus.NotDetermined || this == PermissionStatus.ShouldShowRationale

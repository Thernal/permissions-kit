package io.thernal.permissionskit.permissions.wiring

import androidx.compose.runtime.ProvidedValue
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.thernal.permissionskit.permissions.api.presentation.LocalPermissionStateProvider
import io.thernal.permissionskit.permissions.impl.presentation.platformPermissionStateProvider

/**
 * Contributes the platform's provider as a `ProvidedValue` into the app graph's
 * `Set<ProvidedValue<*>>`, which the application installs once at its root with
 * `CompositionLocalProvider(*values.toTypedArray())`.
 */
@BindingContainer
@ContributesTo(AppScope::class)
interface PermissionsWiring {
    companion object {
        @Provides
        @IntoSet
        fun providePermissionStateProvider(): ProvidedValue<*> {
            return LocalPermissionStateProvider provides platformPermissionStateProvider()
        }
    }
}

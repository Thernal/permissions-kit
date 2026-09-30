package io.thernal.permissionskit.permissions.impl.domain

import io.thernal.permissionskit.permissions.api.domain.AppPermission
import io.thernal.permissionskit.permissions.api.domain.PermissionStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakePlatform(
    vararg initial: Pair<AppPermission, PermissionStatus>,
) : PermissionPlatform {
    val current = mutableMapOf(*initial)
    val peeked = mutableMapOf<AppPermission, PermissionStatus>()
    val requests = mutableListOf<List<AppPermission>>()
    var answer: (AppPermission) -> PermissionStatus = { PermissionStatus.Granted }
    var gate: CompletableDeferred<Unit>? = null
    var settingsOpened = 0

    override fun peek(permission: AppPermission): PermissionStatus {
        return peeked[permission] ?: current[permission] ?: PermissionStatus.NotDetermined
    }

    override suspend fun status(permission: AppPermission): PermissionStatus {
        return current[permission] ?: PermissionStatus.NotDetermined
    }

    override suspend fun request(permissions: List<AppPermission>) {
        requests += permissions
        gate?.await()
        permissions.forEach { current[it] = answer(it) }
    }

    override fun openSettings() {
        settingsOpened++
    }
}

class PermissionStatesControllerTest {
    @Test
    fun `starts from what the platform reports without waiting`() {
        val platform = FakePlatform(AppPermission.Camera to PermissionStatus.Denied)
        platform.peeked[AppPermission.Notification] = PermissionStatus.NotDetermined

        val controller = PermissionStatesController(platform, listOf(AppPermission.Camera, AppPermission.Notification))

        assertEquals(
            mapOf(
                AppPermission.Camera to PermissionStatus.Denied,
                AppPermission.Notification to PermissionStatus.NotDetermined,
            ),
            controller.statuses.value,
        )
    }

    @Test
    fun `refresh replaces a peeked status with the platform's answer`() {
        runTest {
            val platform = FakePlatform(AppPermission.Notification to PermissionStatus.Granted)
            platform.peeked[AppPermission.Notification] = PermissionStatus.NotDetermined
            val controller = PermissionStatesController(platform, listOf(AppPermission.Notification))

            controller.refresh()

            assertEquals(PermissionStatus.Granted, controller.statuses.value.getValue(AppPermission.Notification))
        }
    }

    @Test
    fun `request asks only for what is missing and can change`() {
        runTest {
            val platform = FakePlatform(
                AppPermission.Camera to PermissionStatus.Granted,
                AppPermission.Microphone to PermissionStatus.Restricted,
                AppPermission.PhotoLibrary to PermissionStatus.Limited,
                AppPermission.Notification to PermissionStatus.ShouldShowRationale,
            )
            val controller = PermissionStatesController(platform, AppPermission.entries)

            controller.request()

            assertEquals(listOf(listOf(AppPermission.Notification)), platform.requests)
            assertEquals(PermissionStatus.Granted, controller.statuses.value.getValue(AppPermission.Notification))
        }
    }

    @Test
    fun `request re-reads first so a permission granted in settings is not asked again`() {
        runTest {
            val platform = FakePlatform(AppPermission.Camera to PermissionStatus.Denied)
            val controller = PermissionStatesController(platform, listOf(AppPermission.Camera))
            platform.current[AppPermission.Camera] = PermissionStatus.Granted

            controller.request()

            assertEquals(emptyList(), platform.requests)
            assertEquals(PermissionStatus.Granted, controller.statuses.value.getValue(AppPermission.Camera))
        }
    }

    @Test
    fun `a denial is reported after the request`() {
        runTest {
            val platform = FakePlatform()
            platform.answer = { PermissionStatus.Denied }
            val controller = PermissionStatesController(platform, listOf(AppPermission.Microphone))

            controller.request()

            assertEquals(PermissionStatus.Denied, controller.statuses.value.getValue(AppPermission.Microphone))
        }
    }

    @Test
    fun `a second request while the prompt is showing is ignored`() {
        runTest {
            val platform = FakePlatform()
            platform.gate = CompletableDeferred()
            val controller = PermissionStatesController(platform, listOf(AppPermission.Camera))

            val first = launch { controller.request() }
            runCurrent()
            controller.request()
            platform.gate?.complete(Unit)
            first.join()

            assertEquals(1, platform.requests.size)
        }
    }

    @Test
    fun `open settings goes to the platform`() {
        val platform = FakePlatform()

        PermissionStatesController(platform, listOf(AppPermission.Camera)).openSettings()

        assertEquals(1, platform.settingsOpened)
    }
}

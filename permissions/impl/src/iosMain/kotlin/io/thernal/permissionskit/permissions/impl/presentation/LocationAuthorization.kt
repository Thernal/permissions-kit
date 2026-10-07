package io.thernal.permissionskit.permissions.impl.presentation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.darwin.NSObject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The location authorisation, which iOS reports only through a `CLLocationManager` delegate. The manager
 * holds its delegate weakly and drops a request whose manager is released, so both live as long as this
 * object — created once, on the main thread, by [IosPermissionPlatform].
 */
internal class LocationAuthorization {
    private val manager = CLLocationManager()
    private val changes = MutableStateFlow(manager.authorizationStatus)

    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            changes.value = manager.authorizationStatus
        }
    }

    init {
        manager.delegate = delegate
    }

    val status: CLAuthorizationStatus
        get() = manager.authorizationStatus

    /**
     * Whether the one-time "Change to Always Allow" prompt has been asked for. iOS reports a refused
     * upgrade exactly like one never offered — plain When In Use — so it is recorded here.
     */
    var isUpgradeAsked: Boolean
        get() = NSUserDefaults.standardUserDefaults.boolForKey(UPGRADE_ASKED_KEY)
        private set(value) {
            NSUserDefaults.standardUserDefaults.setBool(value, forKey = UPGRADE_ASKED_KEY)
        }

    suspend fun requestWhenInUse() {
        awaitPrompt { manager.requestWhenInUseAuthorization() }
    }

    /** The upgrade from When In Use. Recorded as asked whether or not iOS still showed it. */
    suspend fun requestAlways() {
        awaitPrompt { manager.requestAlwaysAuthorization() }
        isUpgradeAsked = true
    }

    /**
     * Runs [ask] and returns once the prompt is answered. A system alert makes the app resign active; no
     * resignation soon after means iOS showed nothing (a prompt already used up). A prompt answered
     * without a change — the upgrade declined — ends when the app is active again.
     */
    private suspend fun awaitPrompt(ask: () -> Unit) {
        val before = manager.authorizationStatus
        val resigned = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Unit>()
        val center = NSNotificationCenter.defaultCenter
        val observers = listOf(
            center.addObserverForName(
                name = UIApplicationWillResignActiveNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue,
            ) { _ -> resigned.complete(Unit) },
            center.addObserverForName(
                name = UIApplicationDidBecomeActiveNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue,
            ) { _ ->
                if (resigned.isCompleted) {
                    returned.complete(Unit)
                }
            },
        )
        try {
            coroutineScope {
                val changed = async { changes.first { it != before } }
                ask()
                val isPromptShown = withTimeoutOrNull(PROMPT_APPEARS) {
                    select {
                        resigned.onAwait { true }
                        changed.onAwait { true }
                    }
                } ?: false
                if (isPromptShown && !changed.isCompleted) {
                    select {
                        returned.onAwait { }
                        changed.onAwait { }
                    }
                    // Active again; the delegate may report the answer a moment later.
                    withTimeoutOrNull(ANSWER_SETTLES) { changed.await() }
                }
                changed.cancel()
            }
        } finally {
            observers.forEach { center.removeObserver(it) }
        }
    }
}

private const val UPGRADE_ASKED_KEY = "io.thernal.permissionskit.permissions.locationUpgradeAsked"
private val PROMPT_APPEARS = 1.seconds
private val ANSWER_SETTLES = 500.milliseconds

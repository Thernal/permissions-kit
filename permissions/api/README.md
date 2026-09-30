# permissions/api

The contract a feature uses. Why it has this shape: [`../README.md`](../README.md).

## Dependencies you declare

A feature module that asks for a permission declares `api` and Compose (the kit adds nothing to a
consumer's classpath):

```kotlin
commonMain.dependencies {
    implementation(projects.core.permissions.api)   // the path the kit was installed under
}
```

The application declares `wiring` in the module that owns the app graph, and `testing` in `commonTest`
where a test needs the fake.

## Asking for one permission

```kotlin
val camera = rememberPermissionState(AppPermission.Camera)

when {
    camera.status.isGranted -> CameraPreview()
    camera.status.canRequest -> Button(onClick = camera.request) { Text("Allow the camera") }
    camera.status == PermissionStatus.Denied -> Button(onClick = camera.openSettings) { Text("Open settings") }
    else -> Text("The camera is blocked on this device")   // Restricted
}
```

`PermissionState` is `status`, `request`, `openSettings`. `request()` re-reads the status, shows the
system prompt when the permission is not granted, and updates `status` with the answer; it does nothing
while a prompt is already showing.

| `PermissionStatus` | Means | Offer |
|---|---|---|
| `Granted` | allowed | the feature |
| `Limited` | iOS: allowed for the photos the user picked | the feature; "Select more photos" if it matters |
| `NotDetermined` | never asked | `request` |
| `ShouldShowRationale` | Android: refused once, the system will ask again | an explanation, then `request` |
| `Denied` | refused; only settings can change it | `openSettings` |
| `Restricted` | iOS: blocked by a device policy | nothing — say so |

`isGranted` is `Granted` or `Limited`; `canRequest` is `NotDetermined` or `ShouldShowRationale`.

## Asking on first launch

```kotlin
val notifications = rememberPermissionState(AppPermission.Notification)
LaunchedEffect(Unit) {
    if (notifications.status == PermissionStatus.NotDetermined) notifications.request()
}
```

On iOS the notification status arrives a moment after the first composition, so it may read
`NotDetermined` first; `request()` re-reads before prompting, and never prompts a user who has answered.

## Several at once

```kotlin
val media = rememberPermissionState(listOf(AppPermission.Camera, AppPermission.Microphone))

if (media.areAllGranted) Recorder() else Button(onClick = media.request) { Text("Allow camera and microphone") }
```

`MultiPermissionState` is `statuses` (one entry per permission, in the given order), `request`,
`openSettings`, `areAllGranted`. Android shows one dialog for all of them; iOS asks one after another.

## Settings

`openSettings()` opens this app's page in the system settings. Statuses are re-read when the app comes
back, so the screen changes without any code.

## The platform side

Only the application can declare these, and a request without them fails:

| Permission | Android manifest | iOS `Info.plist` |
|---|---|---|
| `Camera` | `android.permission.CAMERA` (+ `<uses-feature android:name="android.hardware.camera" android:required="false"/>`) | `NSCameraUsageDescription` |
| `Microphone` | `android.permission.RECORD_AUDIO` (+ `uses-feature` `android.hardware.microphone`, not required) | `NSMicrophoneUsageDescription` |
| `PhotoLibrary` | nothing — use the Photo Picker (`PickVisualMedia`) | `NSPhotoLibraryUsageDescription` |
| `Notification` | `android.permission.POST_NOTIFICATIONS` | nothing (push also needs the capability) |

## Installing the provider

`wiring` contributes the platform's provider into the app graph's `Set<ProvidedValue<*>>`. The root
installs that set once:

```kotlin
CompositionLocalProvider(*graph.compositionLocals.toTypedArray()) { App() }
```

Without it every call gets the preview provider: everything `Granted`, nothing prompts.

## Previews

Nothing to do: the default provider grants everything. To preview a refused state, install the fake:

```kotlin
@Preview
@Composable
private fun ScanButtonDeniedPreview() {
    CompositionLocalProvider(
        LocalPermissionStateProvider provides FakePermissionStateProvider(AppPermission.Camera to PermissionStatus.Denied),
    ) { ScanButton(onScan = {}) }
}
```

## Tests

`FakePermissionStateProvider` (`testing`) holds statuses a test sets, records every request in `requests`,
answers it with `answer` (everything granted by default) and counts `settingsOpened`. Install it with
`LocalPermissionStateProvider provides fake`; `fake.stateFor(permissions)` gives the same state without a
composition.

```kotlin
val fake = FakePermissionStateProvider(AppPermission.Camera to PermissionStatus.NotDetermined, answer = { PermissionStatus.Denied })
fake.stateFor(listOf(AppPermission.Camera)).request()
assertEquals(PermissionStatus.Denied, fake.status(AppPermission.Camera))
```

## Adding a permission

1. An entry in `AppPermission` (`api`).
2. Its manifest permission in `AppPermission.manifestPermission()` (`impl/androidMain`) — `null` when the
   Android version asks for none.
3. Its status and request in `IosPermissionPlatform.peek` / `requestOne` (`impl/iosMain`), and a mapping
   of the framework's status next to `avStatus`/`photoStatus`.
4. The manifest entry and usage description in the application.

The compiler points at every `when` that needs the new entry. The change is local to the application's
copy; `kit update` merges it with later kit changes.

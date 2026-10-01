# Usage

## One permission

```kotlin
val camera = rememberPermissionState(AppPermission.Camera)
when {
    camera.status.isGranted -> CameraPreview()
    camera.status == PermissionStatus.ShouldShowRationale -> Rationale(onContinue = camera.request)
    camera.status.canRequest -> Button(onClick = camera.request) { Text("Allow the camera") }
    camera.status == PermissionStatus.Denied -> Button(onClick = camera.openSettings) { Text("Open settings") }
    else -> Text("Blocked on this device")   // Restricted
}
```

## Several

```kotlin
val media = rememberPermissionState(listOf(AppPermission.Camera, AppPermission.Microphone))
if (media.areAllGranted) Recorder() else Button(onClick = media.request) { Text("Allow both") }
```

`media.statuses[AppPermission.Microphone]` gives one. Android: one dialog; iOS: one prompt after another.

## First launch (notifications)

```kotlin
val notifications = rememberPermissionState(AppPermission.Notification)
LaunchedEffect(Unit) {
    if (notifications.status == PermissionStatus.NotDetermined) notifications.request()
}
```

Safe on iOS although the first status may be stale: `request()` re-reads before prompting.

## From a ViewModel

Permissions are UI: the screen owns the state. A ViewModel that needs to know sends an intent
(`CameraGranted`) from the screen, or the screen gates the action before posting it. Never pass a
`PermissionState` into a ViewModel.

## Previews

The default provider grants everything. For another state:

```kotlin
CompositionLocalProvider(
    LocalPermissionStateProvider provides FakePermissionStateProvider(AppPermission.Camera to PermissionStatus.Denied),
) { ScanScreen() }
```

## Tests

```kotlin
val fake = FakePermissionStateProvider(answer = { PermissionStatus.Denied })
val state = fake.stateFor(listOf(AppPermission.Camera))
state.request()
assertEquals(listOf(listOf(AppPermission.Camera)), fake.requests)
assertEquals(PermissionStatus.Denied, state.statuses[AppPermission.Camera])
fake.set(AppPermission.Camera, PermissionStatus.Granted)   // the user changed it in settings
```

In a Compose UI test install it with `LocalPermissionStateProvider provides fake`.

## Adding a permission

1. `AppPermission` (`api`): the entry, with a KDoc line on what it unlocks.
2. `AppPermission.manifestPermissions()` (`impl/androidMain`): the manifest strings, empty when the
   Android version asks for none.
3. `IosPermissionPlatform` (`impl/iosMain`): `peek` (the status now) and `requestOne` (the prompt), plus a
   status mapping beside `avStatus`/`photoStatus`, with a test in `iosTest`.
4. The app's manifest entry and `Info.plist` usage description.

The compiler flags each `when` that misses the new entry. A framework that answers only through a delegate
(as Core Location does) needs it kept alive past the prompt — `LocationAuthorization` is the example.

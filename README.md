# permissions-kit

Runtime permissions for a Compose Multiplatform app (android, iosArm64, iosSimulatorArm64): one composable
call reads a permission's status, asks for it, and sends the user to settings once it is refused — on
Android through the activity-result launcher, on iOS through AVFoundation, Photos and UserNotifications.

```kotlin
@Composable
fun ScanButton(onScan: () -> Unit) {
    val camera = rememberPermissionState(AppPermission.Camera)
    when {
        camera.status.isGranted -> Button(onClick = onScan) { Text("Scan") }
        camera.status.canRequest -> Button(onClick = camera.request) { Text("Allow the camera") }
        else -> Button(onClick = camera.openSettings) { Text("Open settings") }
    }
}
```

Statuses are re-read whenever the app returns to the foreground, so a change made in settings shows at once.

## Documentation

| Read | For |
|---|---|
| this file | what is here and how it is built |
| [`permissions/api/README.md`](permissions/api/README.md) | the contract, task by task: asking, several at once, rationale, settings, previews, tests, adding a permission |
| [`permissions/README.md`](permissions/README.md) | why each part has its shape, and what changed from the apps it came from |
| [`skills/permissions-kit`](skills/permissions-kit/SKILL.md) | the same for an agent working in an app that uses the kit |

## For AI agents

An application takes the kit by copy: `skillctl.sh kit install permissions-kit --package <its package>
--module <its module path> --alias <its plugin alias>` copies the modules below renamed, installs the
`permissions-kit` skill, and records both in `kits.lock`. `kit.yml` lists what the copied modules expect —
including the manifest entries and `Info.plist` usage descriptions only the application can declare.

## Layout

| Module | Holds | Depends on |
|---|---|---|
| `permissions/api` | `AppPermission`, `PermissionStatus`, `PermissionState`, `MultiPermissionState`, `PermissionStateProvider`, `LocalPermissionStateProvider`, `rememberPermissionState` | Compose |
| `permissions/impl` | the shared status controller; the Android provider (launcher, request ledger); the iOS provider | api, lifecycle, activity-compose and core (Android) |
| `permissions/wiring` | the provider as a `ProvidedValue` in the app graph | api, impl, Metro |
| `permissions/testing` | `FakePermissionStateProvider` | api |
| `sample/shared`, `sample/android`, `sample/ios` | one screen asking for every permission, on both platforms | the kit — never copied |

## Building

```sh
./gradlew build
```

Every target, tests on the JVM host and the iOS simulator — the controller (re-read before asking, asking
only what is missing, one prompt at a time), Android's status rules and manifest mapping, iOS's status
mappings, the fake — Detekt, which fails on any finding (`-PdetektAutoCorrect=true` fixes formatting
first), Android lint on the sample, and the sample's iOS framework link. The Gradle daemon runs on JDK 21
(Metro).

The iOS sample is an Xcode project (`sample/ios/Sample.xcodeproj`, generated from `project.yml` by
XcodeGen); its build phase compiles the framework with Gradle:

```sh
xcodebuild -project sample/ios/Sample.xcodeproj -scheme Sample -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' build
```

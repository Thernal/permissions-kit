# Setup

## Modules

`skillctl.sh kit install permissions-kit --package <app package> --module <path, e.g. :core:permissions> --alias <plugin alias>`
copies `api`, `impl`, `wiring`, `testing` under that path. They apply the application's `kmp.library`,
`compose` and `injection` conventions (build-kit provides them) and need these catalog entries:
`compose-runtime`, `compose-foundation`, `compose-ui`, `lifecycle-common`, `lifecycle-runtime-compose`,
`androidx-activity-compose`, `androidx-core`, `kotlinx-coroutines-core`, `metro-runtime` and the Metro plugin.

Without skill-manager, the kit's `README.md` → Installing → *Without it* does the same by hand (copy, rename, provide).

| Module | Who depends on it |
|---|---|
| `api` | every feature that asks for a permission |
| `impl` | only `wiring` |
| `wiring` | the module that owns the app graph |
| `testing` | `commonTest` of features that test permission-gated logic |

## The root

`PermissionsWiring` contributes `LocalPermissionStateProvider provides <platform provider>` into
`Set<ProvidedValue<*>>`. The app graph exposes that set and the root installs it once:

```kotlin
@DependencyGraph(AppScope::class)
interface AppGraph {
    val compositionLocals: Set<ProvidedValue<*>>
}

@Composable
fun App(graph: AppGraph) {
    CompositionLocalProvider(*graph.compositionLocals.toTypedArray()) { AppContent() }
}
```

With arch-kit the same set is installed by `ComponentLocals(…)`; nothing extra is needed.

On Android the root must be inside a `ComponentActivity` (the launcher needs an activity-result
registry) — `setContent { App(graph) }` is.

## Manifest and Info.plist

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
<uses-feature android:name="android.hardware.microphone" android:required="false" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />  <!-- BackgroundLocation only -->
```

```xml
<key>NSCameraUsageDescription</key><string>…why the app needs the camera…</string>
<key>NSMicrophoneUsageDescription</key><string>…</string>
<key>NSPhotoLibraryUsageDescription</key><string>…</string>
<key>NSLocationWhenInUseUsageDescription</key><string>…</string>
<key>NSLocationAlwaysAndWhenInUseUsageDescription</key><string>…</string>  <!-- BackgroundLocation only -->
```

Only for the permissions the app uses. The kit's `sample/` has both files complete.

## Checks

- A request on iOS crashes with "This app has crashed because it attempted to access privacy-sensitive
  data without a usage description" → the `Info.plist` key is missing.
- Every status is `Granted` on a device → the provider is not installed at the root.
- `request()` does nothing on Android and the status stays `NotDetermined` → the manifest lacks the
  permission (the system answers "denied" without a dialog).

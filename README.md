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

## Installing

An application takes the kit **by copy**, not as a dependency: the code is copied into the app, renamed to the app's own package, and belongs to the app from then on. Nothing is published to a Maven repository.

### With skill-manager

If you have access to the author's knowledge repository (`github.com/Thernal/knowledge`), its **skill-manager** skill does all of it — copy, rename, the skill, and later updates:

```sh
skillctl.sh kit install permissions-kit --package com.example.app --module :core:permissions \
    --alias app
```

It copies the `code` parts of [`kit.yml`](kit.yml) renamed, installs the `permissions-kit` skill and records the copy in `kits.lock`. `kit status` then shows what changed upstream and what the app edited; `kit update` merges the kit's changes three ways, keeping the app's edits. The install prints what the app must provide (`requires`).

### Without it

The same by hand, from a clone of this repository.

1. **Copy** the paths listed under `code` in [`kit.yml`](kit.yml) into the app, under the module path the app gives them: `permissions/…` → `core/permissions/…`. Note the commit you copied (`git rev-parse HEAD`) — updates start from it.
2. **Rename** in everything copied:

   | In the kit | Becomes | Where |
   |---|---|---|
   | `io.thernal.permissionskit` | the app's package, e.g. `com.example.app` | sources, build files; and the directories `io/thernal/permissionskit` |
   | `:permissions:` and `":permissions"`, `projects.permissions.` | the module path, e.g. `:core:permissions:`, `projects.core.permissions.` | build files |
   | `libs.plugins.permissionskit.` | the app's catalog alias, e.g. `libs.plugins.app.` | build files |

   ```sh
   # in the app, after copying — perl, so it runs the same on macOS and Linux
   grep -rlI -e io.thernal.permissionskit -e io/thernal/permissionskit -e :permissions -e plugins.permissionskit. core/permissions \
     | xargs perl -pi -e 's/\Qio.thernal.permissionskit\E/com.example.app/g; s{\Qio/thernal/permissionskit\E}{com/example/app}g; s/\Q:permissions:\E/:core:permissions:/g; s/"\Q:permissions\E"/":core:permissions"/g; s/projects\.\Qpermissions\E\./projects.core.permissions./g; s/libs\.plugins\.\Qpermissionskit\E\./libs.plugins.app./g'
   find core/permissions -depth -type d -path '*/io/thernal/permissionskit' | while read -r d; do
     mkdir -p "${d%/io/thernal/permissionskit}/com/example" && mv "$d" "${d%/io/thernal/permissionskit}/com/example/app"
   done
   find core/permissions -depth -type d -empty -delete
   ```

3. **Provide** what the copy expects — the `requires` list in [`kit.yml`](kit.yml): convention plugins (build-kit's, or the ones in this repository's `build-logic/convention`), catalog entries, settings — and, where listed, platform setup.
4. **The skill** (optional): copy [`skills/permissions-kit`](skills/permissions-kit) into the app's skills directory (`.claude/skills/` for Claude Code), with the same renames, so an agent working in the app knows the kit.
5. **Updates** are yours to carry: `git diff <the commit you copied> <a newer one> -- <the code paths>` in the kit shows what changed; apply what you want, renamed the same way.

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

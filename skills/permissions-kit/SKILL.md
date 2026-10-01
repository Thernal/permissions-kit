---
name: permissions-kit
description: Writes, reviews and debugs runtime-permission code in Compose Multiplatform apps that use permissions-kit (packages io.thernal.permissionskit.permissions.*; rememberPermissionState, PermissionState, MultiPermissionState, AppPermission, PermissionStatus, isGranted, canRequest, openSettings, LocalPermissionStateProvider, FakePermissionStateProvider, PermissionsWiring). Use it for any permission work in such a project, even when the kit is not named - asking for the camera, microphone, photos, notifications or location (foreground or "Always"), a rationale or "open settings" screen, asking on first launch, permission-gated previews and tests, a missing usage description or manifest entry, or adding a new permission. Not for projects without permissions-kit.
---

# permissions-kit

permissions-kit reads and requests runtime permissions from Compose on Android and iOS:
`rememberPermissionState(AppPermission.Camera)` gives `status`, `request`, `openSettings`. Guide:
https://github.com/Thernal/permissions-kit — `permissions/api/README.md`, `permissions/README.md` (why).

## 1. Orient first

```sh
grep -rn --include=*.kt -e "rememberPermissionState(" . | head     # existing call sites — copy their shape
grep -rn --include=*.kt -e "PermissionsWiring" -e "compositionLocals" . # is the provider installed at the root?
grep -rn -e "uses-permission" --include=AndroidManifest.xml .
grep -rn -e "UsageDescription" --include=Info.plist .
```

Nothing installed → [references/setup.md](references/setup.md). **Taken as a kit?** A `kits.lock` naming
`permissions-kit` means the modules were copied renamed with skill-manager — this skill too. `skillctl.sh
kit status permissions-kit` shows what moved upstream; offer `kit update permissions-kit` rather than hand
edits.

## 2. The model

- A feature depends on `permissions/api` only and calls `rememberPermissionState(permission)` or
  `rememberPermissionState(listOf(…))`.
- Status: `Granted`, `Limited` (iOS photos), `NotDetermined`, `ShouldShowRationale` (Android), `Denied`,
  `Restricted` (iOS). Branch on `isGranted` / `canRequest` / `Denied`.
- `request()` re-reads, then prompts for what is missing; `openSettings()` is the way out of `Denied`.
  Statuses are re-read on every resume.
- `wiring` installs the real provider through the app graph's `Set<ProvidedValue<*>>`; without it
  everything is `Granted` (previews).

## 3. Tasks

| Task | Read |
|---|---|
| install, the root, manifest and `Info.plist` entries | [setup.md](references/setup.md) |
| ask, several at once, rationale, settings, first launch, previews, tests, a new permission | [usage.md](references/usage.md) |

## 4. Rules

- Never call Android's `ActivityCompat`/`checkSelfPermission` or iOS's `AVCaptureDevice`/`PHPhotoLibrary`
  authorization from a feature — the kit's state is the one source.
- Never request the photo library on Android: `PhotoLibrary` is always granted there; use the Photo Picker.
- Every permission used needs its manifest entry and `Info.plist` usage description — iOS terminates the
  app on a request without one.
- Handle every status: a screen stuck on `Denied` with a "Request" button that does nothing is a bug —
  offer `openSettings`.
- `BackgroundLocation` is asked through its own state: `request()` takes the foreground grant first, then
  the upgrade (Android 11+: a settings page). Never request `ACCESS_BACKGROUND_LOCATION` or
  `requestAlwaysAuthorization` by hand.
- Ask in context (when the user taps the feature), not all at once at start — except notifications, where
  first launch is usual.
- In tests and previews use `FakePermissionStateProvider`; never mock platform classes.

## 5. Verify

```sh
./gradlew build
```

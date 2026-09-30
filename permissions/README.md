# permissions — design

Why the kit has the shape it has. It is ArenaGo's `core/permissions` (Android-only, a render contract)
and Act2Act's multiplatform `permission` package (expect/actual, iOS, refresh on resume) combined, with
the choices where they differed decided explicitly (epic decisions D45–D49).

## A contract and a composition local

A feature calls `rememberPermissionState(…)` from `api` and never sees a launcher, a `Context` or a
Cocoa framework: `PermissionStateProvider` is a contract, `LocalPermissionStateProvider` carries the
implementation, `wiring` installs the platform's. This is ArenaGo's shape rather than Act2Act's
`expect fun` because the local is what makes a preview and a test possible: without `wiring` the preview
provider grants everything and never prompts, and a test installs `FakePermissionStateProvider` and
sets the statuses it needs. An `expect fun` would bind every caller to the real system dialogs.

## One controller, two platforms

The part that decides — read before asking, ask only for what is missing, one request at a time — is
`PermissionStatesController` in `commonMain`, over a small `PermissionPlatform` interface: `peek` (the
status without waiting), `status`, `request`, `openSettings`. Each platform implements only that, so the
behaviour is the same on both and is tested once, on both backends.

`request()` re-reads first. The user may have granted the permission in settings since the screen last
looked; asking again would show a dialog for nothing, or on iOS do nothing at all.

A second `request()` while prompts are showing is dropped. Android would queue a second dialog behind the
first; iOS shows one system alert at a time, so the kit asks for several permissions one after another.

## Refreshed on resume

Statuses are re-read on every `ON_RESUME` (Act2Act). Settings is outside the app: the only moment a change
there can be seen is the return. A lifecycle that is already resumed replays the event to a new observer,
so the same effect is also the first read.

## Denied or never asked, on Android

Android reports "refused, and I will not ask again" exactly like "never asked", like a one-time grant
("Only this time") that has lapsed, and like a permission set to "Ask every time" in settings: not
granted, no rationale. Only the last three can still be asked. ArenaGo told them apart with a
`remember`ed flag, lost as soon as the screen left the composition; Act2Act compared with the previous
status, lost on restart. The first version of the kit recorded every permission it had asked for and
called any later "not granted, no rationale" `Denied` — wrong after a one-time grant lapsed: the app
offered settings where the system would simply have asked again.

So the kit records a **refusal**, not a request (`RequestLedger`, a small `SharedPreferences` file), and
decides it from how a request ended (D47, refined):

- No dialog at all — the system answered by itself: refused for good. The host activity pauses even
  then (the system starts and finishes an invisible activity), but for about a tenth of a second; a
  dialog someone answers keeps it paused far longer, so a pause under 300 ms counts as no dialog.
- Refused in the dialog after having refused once (`ShouldShowRationale` before): Android's second
  refusal, final.
- Anything else — granted (one-time included), a first refusal (a rationale follows), a first dialog
  dismissed without an answer — is not a refusal, and clears one.

`openSettings()` clears the refusal too: the user may set the permission to "Ask every time" there. If
they did not, the next `request()` comes back without a dialog and records it again — one tap that shows
nothing, then `Denied`. iOS reports `NotDetermined` itself and needs no record.

Checked on an Android 15 emulator, before and after: one-time grant lapsed, two refusals, settings set
to "Ask every time", settings left unchanged, a first dialog dismissed.

## The photo library on Android

`PhotoLibrary` is `Granted` on Android and never asks (D48). The system Photo Picker needs no permission,
and Google Play restricts `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO` to apps whose core purpose is a gallery.
ArenaGo asked for them; Act2Act had already stopped. An app that really needs broad media access adds its
own entry.

iOS limited photo access is its own status, `Limited`, which `isGranted` accepts: the feature works, and a
screen can offer "Select more photos". ArenaGo and Act2Act reported it as `Granted`.

## Notifications below Android 13

No permission exists before Android 13, but the user can still turn notifications off. The status there
follows `NotificationManagerCompat.areNotificationsEnabled()` — `Granted` or `Denied` — so a screen that
shows "notifications are off" is right on every version.

## iOS notification status is asynchronous

`UNUserNotificationCenter` answers only through a callback, so the first composition shows the last status
read in this process (`NotDetermined` before any) and the real one a moment later. `request()` re-reads
before deciding, so asking on a splash screen when the status is `NotDetermined` never prompts a user who
has already answered.

## What changed from the sources

- `allGranted` → `areAllGranted` (the kit's Detekt naming rule); `ImmutableList`/`ImmutableMap` → `List`/`Map`
  (no kotlinx-collections-immutable dependency for callers; the state types are `@Stable` interfaces).
- `Microphone` is new (D46). `Limited` and `openSettings` are new (D48).
- The provider has one method; the single-permission overload is built on it in `api`.

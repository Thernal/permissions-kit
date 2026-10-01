package io.thernal.permissionskit.permissions.api.domain

/**
 * A permission a feature asks for, named for what it unlocks rather than for a platform constant.
 * `impl` maps each entry to the Android manifest permission and the iOS framework call.
 *
 * Adding an entry means adding its mapping on both platforms (`impl`'s `androidMain` and `iosMain`) —
 * the compiler points at both `when`s — and its manifest entry and `Info.plist` usage description in
 * the application.
 */
enum class AppPermission {
    /** Posting notifications. Android 13+ asks; below it the status follows the app's notification setting. */
    Notification,

    /** The camera. */
    Camera,

    /** Recording audio. */
    Microphone,

    /**
     * Reading the photo library. Always [PermissionStatus.Granted] on Android: the system Photo Picker
     * needs no permission. iOS asks, and may answer [PermissionStatus.Limited].
     */
    PhotoLibrary,

    /**
     * The device's location while the app is in use: precise or approximate on Android, "While Using the
     * App" on iOS. Either accuracy counts as granted.
     */
    Location,

    /**
     * The location at any time, the app in the background included — "Allow all the time" on Android,
     * "Always" on iOS. Both systems grant it only on top of [Location], so `request()` asks for that first
     * when it is missing, then for this: a second dialog, and on Android 11+ a page in the system settings.
     * Below Android 10 the [Location] grant covers it.
     *
     * Each system shows its upgrade prompt once: [PermissionStatus.Denied] afterwards, while [Location]
     * stays granted, means only settings can change it.
     */
    BackgroundLocation,
}

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
}

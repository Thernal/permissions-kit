# Location and contacts

**Status:** open

`AppPermission` has the four permissions the apps used (D46). Location is the likely next one: Android
has coarse and fine (and background, asked separately), iOS answers through a `CLLocationManager`
delegate that must stay alive until the user answers, and "when in use" versus "always" is a second
question. Contacts is simpler (`READ_CONTACTS`, `CNContactStore`). Add them when an app needs them —
the recipe is in `permissions/api/README.md` → Adding a permission.

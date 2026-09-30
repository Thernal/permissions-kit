# Run the sample on a device

**Status:** open

`./gradlew build` compiles and lints the Android sample and links the iOS framework; `xcodebuild` builds the
iOS sample, and it launches on the simulator and shows every permission as not asked. Nobody has yet tapped through it: each permission's prompt, a refusal, a
second refusal (Android's `Denied`), a change in settings picked up on return, limited photo access on iOS.
Do it once on each platform and note what was seen here.

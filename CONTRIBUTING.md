# Contributing

Thanks for helping! Rules of the road:

1. **Privacy first.** No network access, no trackers, no closed-source dependencies.
2. **Honest claims.** Do not describe the app as "preventing" access it can only detect.
3. **Public APIs only.** If a feature needs root, hidden APIs or a vendor ROM, discuss it in an issue first.
4. Keep it small and readable: Kotlin, Jetpack Compose, no DI framework, no ORM.
5. Add or update strings in both `values/strings.xml` and `values-ro/strings.xml` (more languages welcome).

## Dev setup
Open the folder in Android Studio (Ladybug or newer, JDK 17) and run the `app` configuration
on a device or emulator (API 26+). A real device is recommended: emulators do not behave like
real camera/microphone hardware.

Command line: `gradle :app:assembleDebug` (Android Studio creates the Gradle wrapper for you;
you may commit it with `gradle wrapper`).

## Testing checklist for a PR
- Camera: open the camera app -> "Camera started"; close it -> "Camera stopped".
- Microphone: record a voice memo -> start / stop events.
- Turn the screen off while a recorder runs -> event flagged "Suspicious" + alert.
- Phone call with screen off -> must NOT be flagged.
- Reboot -> monitoring resumes if it was on.

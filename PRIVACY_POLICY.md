# Privacy Policy

_Last updated: 23 November 2026_

**Short version: Sentinel collects nothing and sends nothing. Everything stays on your phone.**

## What data the app handles
Sentinel keeps a local event log: when the camera or microphone started or stopped, whether the
screen was on, whether the event was flagged suspicious, and (optionally) the package name of the app
that was probably in the foreground.

## Where it is stored
In the app's private storage on your device (a local SQLite database and a small settings file).
It is excluded from Android cloud backup and device-to-device transfer. You can erase it at any time with
**Clear log** or by uninstalling the app. The log keeps at most the latest 5,000 events.

## What leaves your device
Nothing. The app does not declare the `INTERNET` permission, so it cannot make network connections.
There are no analytics, advertising, crash-reporting or other third-party services.

## What Sentinel never does
- It never records audio, video or images.
- It never reads the contents of your messages, files, contacts or location.
- It never opens the camera or the microphone itself.

## Permissions and why they exist
| Permission | Why |
|---|---|
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Keep monitoring running in the background, with a visible notification. |
| `POST_NOTIFICATIONS` | Show the monitoring notification and alerts (Android 13+). |
| `RECEIVE_BOOT_COMPLETED` | Resume monitoring after a reboot, if you enabled it. |
| `PACKAGE_USAGE_STATS` (optional, user-granted) | Guess which app was in the foreground when the camera or microphone started. |
| `QUERY_ALL_PACKAGES` | List which installed apps can use the camera or microphone. |

## Children
The app is not directed at children and collects no personal data from anyone.

## Changes
Any change to this policy will be published in this repository and noted in the [changelog](CHANGELOG.md).

## Contact
Open an issue on the repository, or use the private security reporting described in [SECURITY.md](SECURITY.md).

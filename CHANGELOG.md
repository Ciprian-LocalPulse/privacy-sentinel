# Changelog

All notable changes are documented here. Format based on [Keep a Changelog](https://keepachangelog.com/),
versioning follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [0.1.0] - 2026-11-23
### Added
- Foreground service that detects camera and microphone start/stop through public Android APIs.
- Local event log (SQLite), capped at 5,000 events.
- "Suspicious" flag and alert for camera/microphone use with the screen off (calls excluded).
- Optional probable-app attribution via Usage access.
- App audit: which installed apps can use the camera or microphone, with a shortcut to their settings.
- Jetpack Compose UI in English and Romanian.
- Restart after reboot.
- Documentation: README, whitepaper, threat model, privacy policy, security policy.
- CI: build workflow, CodeQL, Dependabot.

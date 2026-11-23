# Manual test plan

Sentinel has not yet been validated across devices. This plan is how to do it. Contributors: please add rows
to the results table via pull request.

## Setup
1. Install the APK, enable **Monitoring**, grant notifications.
2. Optionally grant **Usage access**.
3. Exclude Sentinel from battery optimization (Android Settings → Apps → Sentinel → Battery).

## Cases

| # | Action | Expected |
|---|---|---|
| T1 | Open the camera app, then close it | "Camera started" then "Camera stopped" |
| T2 | Record a voice memo, stop it | "Microphone started" then "Microphone stopped" |
| T3 | Start a voice recorder, turn the screen off | Mic event flagged **Suspicious**, alert shown |
| T4 | Make a phone call with the screen off | **Not** flagged |
| T5 | VoIP call (any messenger) with the screen off | **Not** flagged |
| T6 | Switch on "Alert on every use", open the camera | Alert notification appears |
| T7 | Grant Usage access, open a video-call app that uses the camera | Event shows "Probably: <app>" |
| T8 | Reboot the phone with Monitoring on | Monitoring resumes without opening the app |
| T9 | Open the Apps tab | Apps with camera/mic permission listed; "Settings" opens the correct app page |
| T10 | Clear log | List becomes empty |
| T11 | Lock the phone for 30 minutes, then use the camera | Event still logged (checks OEM service killing) |
| T12 | Rotate screen / switch dark mode | No crash, state preserved |

## Results (add your device)

| Device | Android | Sentinel | T1 | T2 | T3 | T4 | T5 | T6 | T7 | T8 | T9 | T10 | T11 | T12 | Notes |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| _example: Pixel 8_ | 15 | 0.1.0 | | | | | | | | | | | | | not yet tested |

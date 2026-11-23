# Threat model

This document says what Sentinel can and cannot detect. Being precise about this matters more than any feature.

## Assets
- The user's privacy: audio and video of their surroundings.
- The integrity of Sentinel's own log (so that it does not give false reassurance).

## Adversaries

| ID | Adversary | Example | Can Sentinel see it? |
|----|-----------|---------|----------------------|
| A1 | Ordinary app with camera/microphone permission | A game or social app that records more than it should | **Yes**, usually: the OS reports the sensor as busy while it records |
| A2 | Stalkerware / abusive app with elevated app-level rights (accessibility, device admin, notification access) | A hidden "parental control" app installed by an abuser | **Mostly yes** when it uses the camera or microphone through normal APIs; it does not stop it from running |
| A3 | Malware with root, or system-level / vendor spyware, or a compromised OS | Mercenary spyware exploiting the kernel | **No guarantee.** Such software can bypass or falsify what Sentinel observes |
| A4 | Firmware / baseband / hardware-level access | Backdoored modem, microphone wired to another chip | **No.** Outside the reach of any app |
| A5 | Someone with physical access to an unlocked phone | A partner or colleague | **Partly.** Sentinel can show past use, but they can also uninstall or clear it |

## What Sentinel promises
- It reports camera/microphone use that Android itself exposes through public APIs.
- It flags use with the screen off as suspicious, except during calls.
- It never sends data off the device (no internet permission).

## What Sentinel does NOT promise
- Prevention. It cannot block or kill other software's access.
- Completeness. Use that Android does not report to ordinary apps will not appear.
- Attribution. The "probable app" is a heuristic.
- Protection against A3 and A4 adversaries.

## Known weaknesses and mitigations

| Weakness | Effect | Mitigation / status |
|---|---|---|
| Service killed by OEM battery optimization | Events missed while it is dead | Foreground service with notification; user should exclude the app from battery optimization. Detecting gaps is on the roadmap |
| Events before the service started | Not logged | By design; only changes after start are tracked |
| Camera "busy" for non-app reasons | False positives | Documented; tracked as heuristic |
| Microphone callbacks expose limited info for other apps | Cannot always identify who records | Documented; attribution is best-effort |
| Foreground-app attribution wrong | Wrong app named | Marked "Probably" in the UI |
| User uninstalls or clears the app | Log lost | Out of scope |
| Malicious app abuses Sentinel's non-exported components | None expected | Only the launcher activity is exported |

## Out of scope
Root detection, tamper-proofing against system-level attackers, network monitoring, content inspection.

## Residual risk
A user who sees an empty log may conclude they are safe. They may not be: see A3 and A4. The app's
Settings screen and README state this plainly.

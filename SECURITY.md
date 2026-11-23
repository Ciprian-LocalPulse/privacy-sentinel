# Security Policy

Sentinel is a privacy tool, so security reports are taken seriously.

## Supported versions

| Version | Supported |
|---------|-----------|
| 0.1.x   | Yes (current) |
| < 0.1   | No |

## Reporting a vulnerability

**Please do not open a public issue for security problems.**

Use GitHub's private reporting: go to the repository's **Security** tab and choose
**Report a vulnerability**. (Maintainers: enable it under Settings → Code security →
*Private vulnerability reporting*.)

Please include: affected version, Android version and device, steps to reproduce, and the impact
you believe it has.

### What to expect
- Acknowledgement within **7 days**.
- A first assessment within **14 days**.
- A fix or mitigation plan within **90 days** for confirmed issues; sooner for severe ones.
- Credit in the release notes if you want it.

### Safe harbor
Good-faith research on your own device, following this policy, will not be met with legal action.
Please do not access other people's data, and do not degrade other people's devices.

## Scope

In scope:
- Anything that makes Sentinel leak data off the device or expose its log to other apps.
- Bypasses of the app's own permission or storage protections.
- Supply-chain issues (build files, CI workflows, dependencies).
- Logic flaws that cause the app to report false safety (for example, a way to make real camera or
  microphone use disappear from the log through public APIs).

Out of scope:
- Anything that requires root, a compromised OS, or physical access to an unlocked device.
- The known limits documented in [docs/THREAT_MODEL.md](docs/THREAT_MODEL.md): Sentinel is a detector and
  cannot see activity hidden by system-level software.
- Vulnerabilities in Android itself or in third-party libraries (report those upstream).

## Security design principles
1. **No `INTERNET` permission.** A pull request that adds it will be rejected.
2. **No third-party SDKs**, analytics, crash reporters or ads.
3. **Local data only**, in app-private storage, excluded from backups and device transfer.
4. **Public Android APIs only.** No root, no hidden-API reflection.
5. **Minimal permissions**, each justified in [PRIVACY_POLICY.md](PRIVACY_POLICY.md).
6. **Non-exported components** except the launcher activity.

## Verifying a release
Release APKs are built by GitHub Actions from the tagged commit. Each release ships a
`SHA256SUMS.txt`. Verify a download with:

```bash
sha256sum -c SHA256SUMS.txt        # Linux / macOS
Get-FileHash app-debug.apk -Algorithm SHA256   # Windows PowerShell
```

You can also inspect the manifest of any APK to confirm there is no internet permission:

```bash
aapt dump permissions app-debug.apk
```

## Supply chain
- Dependencies are limited to AndroidX, Jetpack Compose and Kotlin standard libraries.
- Dependabot watches Gradle dependencies and GitHub Actions.
- CodeQL scans the code on every push and weekly.
- See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

# Security policy

## Reporting a vulnerability
Please do **not** open a public issue for security problems. Use GitHub's
"Report a vulnerability" (Security tab) or contact the maintainers privately.

## Design principles
- No `INTERNET` permission, ever. A pull request that adds it will be rejected.
- No analytics, no crash reporters, no third-party SDKs.
- All data (the event log) stays in the app's private storage and is excluded from backups.
- Only public Android APIs; no root, no hidden-API reflection.

## What Sentinel does not protect against
Sentinel is a detector, not a shield. It cannot stop spyware that has system-level
access, a compromised OS, or a baseband/firmware backdoor. See the README, section "Limits".

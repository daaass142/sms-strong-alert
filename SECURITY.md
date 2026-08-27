# Security

## Security model

This project deliberately minimizes its attack surface:

- No network permission.
- No WebView.
- No exported activities besides the launcher.
- No always-on service or background polling.
- No dynamic code loading or native libraries.
- No third-party runtime dependencies.
- Local app backup is disabled.

`SmsReceiver` is exported because Android's telephony framework must be able to deliver `SMS_RECEIVED`; the receiver rejects every other action immediately.

## Reporting a vulnerability

Please use GitHub's private vulnerability reporting feature when enabled. Do not include real SMS contents, phone numbers, or license plates in a public issue.

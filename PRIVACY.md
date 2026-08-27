# Privacy

SmsStrongAlert is designed to work entirely on-device.

- The app does **not** declare `android.permission.INTERNET` or `ACCESS_NETWORK_STATE`.
- Incoming SMS content is inspected only in memory to determine whether a local alert should fire.
- Vehicle plates and reminder settings are stored only in the app's private `SharedPreferences`.
- Android cloud backup is disabled (`android:allowBackup="false"`).
- No analytics, ad SDK, crash-reporting SDK, remote config, telemetry, account system, or server API is included.

The app requests `RECEIVE_SMS` because the SMS broadcast itself is the feature trigger. It does not request `READ_SMS` and does not scan SMS history.

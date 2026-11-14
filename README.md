# YaadRide

Android car rental marketplace for Jamaica, built with Kotlin and Jetpack Compose.

## Features

- Separate renter and owner registration and login with Firebase Authentication.
- Vehicle listings with photo galleries, specifications, prices in JMD, and pickup and return locations.
- Search by car or parish and location-based Nearby search.
- Calendar date selection, booking requests, owner approval, and trip details.
- Account-scoped favorites and booking conversations with unread counts.

## Current development status

Accounts and account profiles use Firebase. Vehicle listings, bookings, favorites, and messages currently stay on the device. Cross-device marketplace synchronization, remote message delivery, payments, and payouts are not connected yet.

## Open and run

1. Open this folder in Android Studio and let Gradle sync.
2. Use JDK 21 and install the Android SDK requested by the project.
3. Run the `app` configuration on an emulator or Android device.

The included `app/google-services.json` is Firebase Android client configuration, not an administrator credential. See [ACCOUNT_SETUP.md](ACCOUNT_SETUP.md) for Firebase configuration and account setup details. Never commit service account keys or signing credentials.

On Windows, build outputs are redirected outside OneDrive to avoid file locking issues.

## Checks

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Device tests require a connected Android device. Firebase account integration tests require the local Firebase emulators described in [ACCOUNT_SETUP.md](ACCOUNT_SETUP.md).

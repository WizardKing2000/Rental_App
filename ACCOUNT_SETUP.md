# YaadRide accounts

## Connected Firebase project

- Project: `yaadride-jm-20261101` (YaadRide).
- Android package: `com.example.ra`.
- Email/password authentication is enabled.
- Account profiles use the default Realtime Database in `us-central1`.
- Android configuration is in `app/google-services.json`.
- Firebase CLI login credentials are stored by the CLI on this computer, never in the app.

Console: https://console.firebase.google.com/project/yaadride-jm-20261101/overview

## Account behavior

Renter and Owner have separate registration and login screens. One email identifies
one account with one saved role. Signing in through the wrong portal fails and
signs the Firebase user out.

Registration requires a name, valid email, matching passwords, and a password of
10–128 characters. Passwords are handled by Firebase Authentication and are never
stored in rental preferences or saved UI state.

An authenticated app session is exposed only after its profile has been loaded.
Interrupted profile creation can be recovered by selecting Create account again
and proving the same email/password. Existing profiles cannot have their role
overwritten through registration.

Password reset uses Firebase's email flow. The app restores valid sessions after
relaunch and clears the signed-in view on logout.

## Profile security

Each user can create and read only `accountProfiles/{their Firebase UID}`.
Profiles contain only `name` and `role`; allowed roles are RENTER and OWNER.
Client updates, deletions, unknown fields, and cross-account reads/writes are denied.
The deployed configuration is `firebase.json` and `database.rules.json`.

Deploy deliberate future configuration changes using:

```powershell
npx -y firebase-tools@latest deploy --only "auth,database" --project yaadride-jm-20261101
```

## Rental data remains local

Cars and bookings are still saved on this device. Listings and requests carry
ownerId and renterId. Owners can manage only their own listings and requests;
renters can view and cancel only their own bookings. Local checks must also be
enforced by a backend when rental data is moved online.

Older demo records without account IDs are preserved but are not assigned to
whoever registers first. They cannot be managed or booked under a new account.

Payments and payouts are not connected.

## Validation and local test commands

Verified: debug build and Android lint pass; 13 unit tests, 5 Firebase rules
tests, and the Android account integration test pass. The connected build was
installed on emulator-5554.

The project uses Firebase BoM 34.4.0 to remain compatible with its Kotlin 2.2
compiler. BoM 35.1.0's Authentication library requires newer Kotlin metadata.

Android checks:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Firebase rules tests require Node.js 20+ and Java 21 on PATH:

```powershell
npx -y firebase-tools@latest emulators:exec --config firebase.emulators.json --project demo-yaadride-accounts --only "auth,database" "node --test firebase-tests/account-rules.test.mjs"
```

Android account integration test (requires a running Android emulator):

```powershell
npx -y firebase-tools@latest emulators:exec --config firebase.emulators.json --project demo-yaadride-accounts --only "auth,database" ".\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.ra.AccountSessionIntegrationTest --console=plain"
```

Both Firebase test suites use localhost emulators and a demo project. They create
no real customer accounts and send no emails. Debug network configuration permits
cleartext only for localhost and the Android emulator's host address; release
builds use normal HTTPS requirements.

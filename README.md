# SpoofMyDevice

SpoofMyDevice 2.0 is an Xposed module and companion app for building, saving, and applying spoofed Android device profiles to selected apps.

The companion UI is written entirely in Jetpack Compose and uses Material 3 Expressive components, a Tonal Spot palette, and the 2025 Material color specification.

## Features

- Reusable preset-based and fully editable custom profiles
- Independent app-to-profile assignments
- Dedicated profile management and per-app profile assignment screens
- Live profile propagation to already hooked scoped apps
- Per-field enable controls and one-tap randomization for advanced identifiers
- Safe Mode that keeps the real `Build.VERSION` and `ro.*.build.version.*` values for selected apps
- Model, fingerprint, telephony, and identifier hooks remain enabled in Safe Mode
- Android System Framework is always excluded from hooking
- Optional resolution, density, window metrics, and configuration spoofing
- Locale, timezone, Build ID, and Display ID spoofing
- English, Korean, Japanese, and Simplified Chinese UI
- Runtime diagnostics and app information screens
- Rooted LSPosed and non-root patch-manager workflows

## Requirements

- Android 8.0 or newer
- One of the following module runtimes:
  - LSPosed on a rooted device
  - A non-root patch manager that implements the legacy Xposed API, such as LSPatch or NPatch

## Rooted LSPosed Setup

1. Install the SpoofMyDevice APK and open it once.
2. Enable SpoofMyDevice in LSPosed.
3. Add only the target apps to the module scope. Do not select Android System Framework.
4. Open **Profiles**, create a profile, then select a preset or edit its custom values and save it.
5. Open **Apps**, select a target app, then choose one of the saved profiles in its assignment screen.
6. Force stop and relaunch newly assigned target apps.

## Non-root Setup

1. Install the SpoofMyDevice APK normally and open it once.
2. In LSPatch or NPatch, register SpoofMyDevice as the Xposed module for the target APK.
3. Patch the target APK and install the patched result.
4. Create and save a profile in **Profiles**, then open **Apps**, select the patched app, and assign that profile.
5. Relaunch the patched target app.

Patched APK signatures differ from the original signatures. Apps that enforce signature, installer, or integrity checks may reject a patched build, and an official app update replaces the patched APK.

## Safe Mode

Safe Mode exists to prevent crashes when the spoofed profile reports a newer Android version than the physical device. For selected apps it bypasses only:

- `Build.VERSION` fields
- `ro.*.build.version.*` properties

Model, fingerprint, network, telephony, and identifier spoofing still applies. Android System Framework is never hooked, regardless of Safe Mode selection.

## Configuration

The companion app stores reusable profiles, app assignments, and global runtime options together in one app-private `app_profiles.json` file. The exported config provider returns only the profile assigned to the requesting package. Packages without an assignment are not hooked.

Applying a profile broadcasts a config-change event so already hooked processes can reload it. A newly assigned app must be relaunched because Xposed hooks are installed when its process starts. Some apps also cache identifiers internally and require a force stop after profile changes.

## Build From Source

```bash
git clone https://github.com/BuSung-dev/SpoofMyDevice.git
cd SpoofMyDevice
./gradlew assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Disclaimer

This project is intended for research, testing, and authorized use. Follow applicable laws, platform rules, and the policies of the apps being tested.

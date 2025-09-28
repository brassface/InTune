# InTune

[中文](README.md) | English

A concise Android app project example (package `com.zzy.intune`). This repo ships ready-to-build APKs and demonstrates release signing and publishing.

## Download
- See latest and previous releases: https://github.com/brassface/InTune/releases
- Grab APK from Release Assets: `InTune_*.apk`

## Install
- Enable "install from unknown sources" on your device
- Copy APK to the phone and tap to install

## Compatibility
- Min SDK: Android 5.0 (API 21)
- Target SDK: API 30 (`targetSdkVersion = 30`)

## Build locally
```bash
# Clone via SSH
git clone git@github.com:brassface/InTune.git
cd InTune

# Debug build (unsigned)
./gradlew assembleDebug

# Release build (signed, requires signing props below)
./gradlew assembleRelease
```

Artifacts:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Release signing
Keep secrets out of VCS by using user-level `gradle.properties`.
(Windows default: `C:\Users\<you>\.gradle\gradle.properties`; if `GRADLE_USER_HOME` is set, Gradle also reads `<GRADLE_USER_HOME>\gradle.properties`.)

1) Prepare keystore (if you don't have one):
```bash
# Example (replace path, alias and passwords)
keytool -genkey -v -keystore /path/to/your.keystore -alias release \
  -keyalg RSA -keysize 2048 -validity 36500
```

2) Add the following to `~/.gradle/gradle.properties` (Windows: `C:\Users\<you>\.gradle\gradle.properties`):
```properties
RELEASE_STORE_FILE=/absolute/path/to/your.keystore
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=release
RELEASE_KEY_PASSWORD=your_key_password
```

3) The project `app/build.gradle.kts` already uses these props:
```kotlin
android {
    signingConfigs {
        create("release") {
            val store = project.findProperty("RELEASE_STORE_FILE") as String?
            if (store != null) {
                storeFile = file(store)
                storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String?
                keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String?
                keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String?
            }
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
}
```

### Signing walkthrough (screenshots)
Screenshots are placed at the repo root and should be viewed in order:

![Step 1](./1.jpg)

![Step 2](./2.jpg)

![Step 3](./3.jpg)

![Step 4](./4.jpg)

![Step 5](./5.jpg)

Then run:
```bash
./gradlew clean assembleRelease
```

## Versioning & Release
- Version name: `defaultConfig.versionName` in `app/build.gradle.kts`
- Publish to GitHub:
  1. `git tag vX.Y.Z && git push origin vX.Y.Z`
  2. Create a Release at https://github.com/brassface/InTune/releases/new and upload the APK as an asset

## Contributing
Issues and PRs are welcome.



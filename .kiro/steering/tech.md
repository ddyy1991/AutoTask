# Tech Stack & Build System

## Build System
- **Gradle**: 8.2.0 (Android Gradle Plugin)
- **Kotlin**: 1.8.10
- **Java**: Version 17 (source and target compatibility)

## Android Configuration
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

## Core Dependencies
- **AndroidX Core**: 1.12.0
- **AppCompat**: 1.6.1
- **Material Design**: 1.11.0
- **ConstraintLayout**: 2.1.4
- **Fragment**: 1.6.2
- **ViewModel**: 2.6.2

## Networking & Serialization
- **OkHttp3**: 4.11.0 (HTTP client for API calls)
- **Gson**: 2.10.1 (JSON serialization/deserialization)

## Testing
- **JUnit**: 4.13.2
- **AndroidX Test**: 1.1.5
- **Espresso**: 3.5.1

## External APIs
- **GLM API**: For AI-powered image analysis (API key configured in BuildConfig)

## Repository Configuration
- Maven Central
- Google Maven
- JitPack
- Aliyun Maven mirrors (for faster builds in China)

## Common Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Build and install on device
./gradlew installDebug
```

## Configuration Files
- `gradle.properties`: Project-wide Gradle settings
- `local.properties`: Local SDK/NDK paths (not in version control)
- `gradle/libs.versions.toml`: Centralized dependency version management
- `app/build.gradle`: App-specific build configuration
- `build.gradle`: Root project configuration

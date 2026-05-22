# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android project. App code lives in `app/src/main/java/dev/gitfudge/debbie`, with Kotlin files grouped by responsibility such as `MainActivity.kt`, `ViewModels.kt`, `Data.kt`, `Theme.kt`, and `DebbieApplication.kt`. Android resources are under `app/src/main/res`, including fonts in `res/font` and values in `res/values`. Static brand assets are in `app/src/main/assets`. Unit tests live in `app/src/test/java/dev/gitfudge/debbie`; add instrumentation tests under `app/src/androidTest/java` when device or Android framework behavior must be exercised.

## Build, Test, and Development Commands

- `./gradlew :app:assembleDebug` builds the debug APK.
- `./gradlew :app:installDebug` installs the debug variant on a connected device or emulator.
- `./run.sh` installs and launches `dev.gitfudge.debbie.debug`; set `ANDROID_SERIAL` when multiple devices are connected.
- `./gradlew :app:testDebugUnitTest` runs local JVM unit tests.
- `./gradlew :app:connectedDebugAndroidTest` runs Android instrumentation tests on a connected device.
- `./gradlew :app:assembleRelease` builds the minified release variant using `app/proguard-rules.pro`.

After every code or resource change, run `./run.sh` so the debug app is rebuilt, installed, and launched on the connected device or emulator. If multiple devices are connected, set `ANDROID_SERIAL` first.

## Coding Style & Naming Conventions

Use Kotlin with 4-space indentation and Java 17/JVM 17 compatibility. Follow idiomatic Kotlin naming: `PascalCase` for classes, composables, and enum values; `camelCase` for functions, properties, and local variables; `UPPER_SNAKE_CASE` only for true constants. Keep code in the `dev.gitfudge.debbie` package unless introducing a clearly scoped subpackage. Prefer small, focused Compose functions and ViewModel state holders over large activity-level logic. Add dependencies through `gradle/libs.versions.toml` instead of hardcoding versions in build files.

## Testing Guidelines

The project currently uses JUnit 4 for local tests. Name test classes after the behavior or unit under test, for example `FormatStatusTest`, and keep test method names descriptive, such as `formatsSizesSpeedEtaAndRelativeTime`. Put fast pure Kotlin tests in `app/src/test`; use `app/src/androidTest` for UI, Hilt, DataStore, or platform integration coverage. Run `./gradlew :app:testDebugUnitTest` before opening a PR.

## Commit & Pull Request Guidelines

This repository has no existing commit history to infer conventions from. Use short, imperative commit subjects such as `Add login error handling` or `Fix status formatting`. Keep each commit focused on one logical change. Pull requests should include a brief summary, test results, linked issues when applicable, and screenshots or screen recordings for visible UI changes.

## Security & Configuration Tips

Do not commit API keys, tokens, signing keys, or local device configuration. Keep secrets in local Gradle properties, Android keystore storage, or another approved secret store. Treat generated screenshots and APKs as review artifacts unless they are intentionally part of the repository.

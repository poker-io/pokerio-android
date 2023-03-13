# pokerio-android

[![Build](https://github.com/poker-io/pokerio-android/actions/workflows/android.yml/badge.svg)](https://github.com/poker-io/pokerio-android/actions/workflows/android.yml)
[![Tests](https://github.com/poker-io/pokerio-android/actions/workflows/tests.yml/badge.svg)](https://github.com/poker-io/pokerio-android/actions/workflows/tests.yml)
[![codecov](https://codecov.io/gh/poker-io/pokerio-android/branch/main/graph/badge.svg?token=4QCZNOWFZJ)](https://codecov.io/gh/poker-io/pokerio-android)
[![Lint](https://github.com/poker-io/pokerio-android/actions/workflows/lint.yml/badge.svg)](https://github.com/poker-io/pokerio-android/actions/workflows/lint.yml)
[![ktlint](https://img.shields.io/badge/ktlint%20code--style-%E2%9D%A4-FF4081)](https://pinterest.github.io/ktlint/)

## Building

The easiest way to build the app is through Android Studio (requires Electric
Eel or newer). Gradle will install all the necessary dependencies.

Alternatively you can run the build from a command line with:

```
./gradlew assembleDebug
```

## Testing

Tests are places under `app/src/test` and `app/src/androidTest`. To run unit
tests execute:

```
./gradlew test
```

or for running instrumented tests:

```
./gradlew connectedAndroidTest
```


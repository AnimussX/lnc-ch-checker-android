# LNCrawl Android Client

Native Android client for the `lightnovel-crawler` project. Provides a UI to control LNCrawl backend, manage download queue and store novels locally.

[![Build](https://github.com/<YOUR-ORG>/<YOUR-REPO>/actions/workflows/android-ci.yml/badge.svg)](https://github.com/<YOUR-ORG>/<YOUR-REPO>/actions/workflows/android-ci.yml)
[![Release](https://github.com/<YOUR-ORG>/<YOUR-REPO>/actions/workflows/release.yml/badge.svg)](https://github.com/<YOUR-ORG>/<YOUR-REPO>/actions/workflows/release.yml)

## Features

- Start crawls on a LNCrawl backend (Flask wrapper)
- Multithreaded downloader with Range requests and SAF support
- Foreground service and EMUI/Huawei-friendly behavior
- Optional Huawei Push Kit (HMS) fallback
- Room DB for task persistence
- WorkManager background tasks and cooperative cancellation

## Quickstart

1. Clone this repo into Android Studio.
2. Optional: configure `Prefs.baseUrl` or change in Settings in-app to point to your LNCrawl API (e.g. `http://192.168.0.10:8000`).
3. (Optional) Provide HMS config for Huawei devices: add `agconnect-services.json` to `app/` or add `AGCONNECT_JSON_BASE64` to GitHub Secrets.
4. Build & Run on device.

## CI / GitHub Actions
- `android-ci.yml` builds Debug and Release (if signing provided).
- `release.yml` creates GitHub releases and uploads artifacts (APK/AAB) when pushing a tag.

## Secrets for CI
Create the following GitHub repository secrets if you want full functionality:
- `AGCONNECT_JSON_BASE64` — base64 of your `agconnect-services.json` (optional)
- `ANDROID_KEYSTORE_BASE64` — base64 of your signing keystore (optional)
- `ANDROID_KEYSTORE_PASSWORD` — keystore password
- `ANDROID_KEY_ALIAS` — alias for signing key
- `ANDROID_KEY_PASSWORD` — key password

## Notes
This repository contains HMS integration stubs and documentation; you must supply your HMS config and follow Huawei docs to enable Push Kit.

## License
MIT

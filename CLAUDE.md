# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Single-module Android app (`:app`, package `com.drakahell.claudesearch`): an own-branded "trace and find" tool in the style of Circle to Search. The user invokes it over any screen, traces an object/text with a finger and gets an AI answer. The owner is not a professional developer: work in small, verifiable steps and explain changes in plain Russian.

The step-by-step roadmap, current status and rationale live in `Docs/` (`plan.md`, `architecture.md`, `decisions.md`, `steps/step-NN-*.md`). `Docs/` is git-ignored on purpose — read it for context and update it after each completed step.

## Commands (Windows / PowerShell)

```powershell
.\gradlew.bat :app:assembleDebug                 # build debug APK
.\gradlew.bat :app:installDebug                  # install on running emulator/device
.\gradlew.bat :app:testDebugUnitTest             # JVM unit tests
.\gradlew.bat :app:testDebugUnitTest --tests "com.drakahell.claudesearch.SomeTest"   # single test class
.\gradlew.bat :app:connectedDebugAndroidTest     # instrumented tests (needs device)
.\gradlew.bat :app:lintDebug                     # Android lint
```

In Android Studio prefer the IDE tools (`gradle_build`, `gradle_sync`) over the shell. Run `git` via the shell with `pty = false` — interactive PTY git calls have hung here.

## Build setup

- AGP 9 with built-in Kotlin (no separate `kotlin-android` plugin); only `kotlin.compose` plugin is applied.
- All versions in `gradle/libs.versions.toml`; add dependencies there, never inline. Compose versions come from the Compose BOM.
- `minSdk 26`, `compileSdk`/`targetSdk 37`. Release uses the AGP `optimization {}` block (R8); keep rules are in `app/src/main/keepRules/`.

## Architecture

Jetpack Compose + Material 3, MVVM with unidirectional data flow. Each screen lives in `ui/<feature>/` and is split into:
- `<Feature>UiState` data class + `<Feature>ViewModel` exposing `StateFlow<UiState>` and event handlers;
- `<Feature>Route` composable — obtains the ViewModel (`viewModel()`), collects state with `collectAsStateWithLifecycle`;
- `<Feature>Screen` composable — stateless, takes `uiState` + lambdas, has a `@Preview`.

`MainActivity` only enables edge-to-edge, applies `ClaudeSearchTheme` (dynamic color on Android 12+) and hosts the root route. No DI framework or navigation library yet; screens are separate Activities started via `Intent` factory functions in their companion objects. ViewModels needing dependencies use `viewModelFactory { initializer { ... } }` (see `TraceViewModel.factory`).

Trace feature (`ui/trace`): `TraceActivity` (immersive) → `TraceViewModel` loads a bitmap through `data/image/ImageLoader` (Photo Picker URI or generated `SampleImage`) and crops selections. `TraceScreen` draws the image and overlay itself on `Canvas` using `ImageFit` (view ↔ image coordinate mapping); gestures are turned into a selection by the pure `SelectionMath` (unit-tested). Selections are stored in **image pixel coordinates**, never view coordinates. `TraceScreen` is meant to be reused in step 4 with a real screenshot.

Search (`data/ai`, `data/lens`, `ui/trace/SearchPanel.kt`): after a selection `TraceViewModel` picks the provider via `AppSettings.effectiveProvider()` (DataStore-backed `SettingsRepository`). Gemini goes through Firebase AI Logic (`GeminiVisualSearch`, model `gemini-3.8-flash`, chat session per selection, streamed). Quota/region/access errors (`SearchFailure`) auto-fall back to Google Lens; quota also pauses Gemini (`QuotaPolicy`). Google Lens is shown in-app: `LensWeb` builds an HTML page that POSTs the crop to lens.google.com's upload form inside `LensWebView`; `GoogleLens` hands off to the Google app as a fallback. App-wide singletons live in `ClaudeSearchApplication` (manual DI); App Check is installed there with per-build-type `AppCheckSetup` in `src/debug` / `src/release` (debug token must be registered in the Firebase console per device). `google-services.json` is git-ignored.

Planned direction (see `Docs/decisions.md`): screen capture via the default-assistant role (`VoiceInteractionService`/`VoiceInteractionSession`, with `MediaProjection` as fallback; `AccessibilityService` is ruled out by Play policy); on-device ML Kit for OCR/barcodes. Planned packages: `assistant`, `ml`.

## Conventions

- Code comments, KDoc and commit messages: English. UI strings: Russian, in `res/values/strings.xml` (no hardcoded text in composables). Docs and chat with the user: Russian.
- One commit per completed step: `Step N: <summary>`, then push to `origin main` (https://github.com/Drakahell/Claude-Search).
- Don't recreate Google's branding/visuals — own name and design, similar mechanics only.

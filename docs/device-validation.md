# Glass Device Validation

Validated on 2026-09-01 using a connected **Glass 1**, Android **5.1.1 / API 22**, 640×360.

## Automated checks

- 67 JVM tests: scoring, singles/doubles rules, service transitions, deuce, win detection, undo/history bounds, navigation, persistence failure atomicity, strict checkpoint validation, and raw gesture recognition.
- `lintDebug`, `lintRelease`, `assembleDebug`, and `assembleRelease` pass.
- No `INTERNET` permission is requested.

## On-device checks passed

- Installed and launched both the debug APK and the dedicated-key-signed, non-debuggable v1.0.0 release APK without crashes.
- Verified the 640×360 mode, serve-selection, playing, reset-confirmation, resume, corrupt-recovery, and game-over layouts from device screenshots.
- Exercised the production raw input path with ADB-injected touch streams:
  - increasing-X forward swipe;
  - decreasing-X backward swipe;
  - long press opens reset/exit confirmation without destructive action;
  - backward cancels confirmation;
  - single tap does not alter persisted state.
- Completed doubles transitions for both the opening-serve exception and normal S1 → S2 → side-out behavior.
- Completed singles scoring and side-outs with no server number.
- Verified serving team is rendered on the left after side-outs.
- Verified semantic double-tap undo on device through the privileged debug control interface.
- Played through 10–10, confirmed 11–10 remains active, and confirmed 12–10 completes the game.
- Undid the winning action, re-completed the game, and started a new game through mode selection.
- Verified state recovery after Activity force-stop, full Glass reboot, and an app update.
- Verified scoring/recovery with Wi-Fi disabled and restored Wi-Fi afterward.
- Injected a semantically corrupt checkpoint; the app entered the safe discard flow without crashing.
- Verified exit preserves the active checkpoint.
- Repeated a raw-input start/score/reset flow on the v1.0.0 signed release, verified its package metadata, and confirmed it requests no `INTERNET` permission.

## Final manual field check

ADB's `input tap` process takes about one second to start on this Glass, so it cannot generate two taps inside the app's 300 ms double-tap window. The raw recognizer has unit coverage for double, triple, and quadruple taps, and semantic undo passed on device. Before the first live match, perform one finger-generated double tap on the Glass touchpad and confirm that it undoes exactly one scoring action.

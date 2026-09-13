# Sketchpad

A minimal Android drawing app: open it, draw with your finger, save the result.

It is a standalone Gradle project inside this repository — it shares no code,
configuration, or dependencies with anything else here.

## What it does

- Finger drawing on a full-screen canvas, smoothed so strokes don't look jagged
- Ten-colour palette and a brush-size slider (2–60 px)
- Eraser that rubs out ink without touching the paper underneath
- Undo / redo, and clear (with a confirmation)
- Save the drawing as a PNG into `Pictures/Sketchpad` in the gallery

## Getting the APK

Every push that touches `sketchpad/` runs the **Sketchpad Android CI** workflow.
Open the run in the Actions tab and download the `sketchpad-debug-apk` artifact;
it contains `app-debug.apk`, which installs on any device running Android 7.0
(API 24) or newer once "install from unknown sources" is allowed.

The workflow can also be started by hand from the Actions tab
("Run workflow"), without pushing anything.

## Building locally

Requires JDK 17 and an Android SDK with platform 35 installed.

```bash
cd sketchpad
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

## Layout

```
sketchpad/
├── app/src/main/java/com/guitorte/sketchpad/
│   ├── DrawingView.kt    # the canvas: strokes, eraser, undo history, export
│   └── MainActivity.kt   # palette, brush size, toolbar actions, saving
├── app/src/main/res/      # layout, colours, icons, theme
└── app/build.gradle.kts   # SDK 24–35, Kotlin, view binding
```

`DrawingView` bakes finished strokes into an offscreen bitmap, so drawing stays
smooth however much is on the canvas; the stroke list survives only to let undo
and redo rebuild that bitmap.

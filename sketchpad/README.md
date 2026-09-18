# Sketchpad

An Android app for drawing on top of a photo. The photo floats on an endless
canvas you can pan and zoom freely, so your working area is never limited to the
image's own dimensions.

It is a standalone Gradle project inside this repository — it shares no code,
configuration, or dependencies with anything else here.

## What it does

- Load a photo from the device and draw on it, at any zoom
- Infinite canvas: the photo sits in open space, so you can annotate in the
  margins as well as on the image
- A **Pan / zoom** toggle, always visible in the action bar. On, one finger
  drags the canvas and a double-tap re-frames the photo. Off, you are back in
  whichever drawing mode was active before — brush or eraser, same colour, same
  size
- A **laser pointer**: marks trail behind your finger and rub themselves out a
  moment later, leaving nothing behind — for pointing things out while recording
  or presenting, not for marking the photo up
- An eyedropper that samples any colour on the canvas — photo, ink or backdrop —
  for the brush, or for the background
- A choice of backdrop colour for the endless canvas, remembered between runs
- Colours and brush size retract behind the palette button when you want the
  canvas back; the action bar stays put, with undo and redo at the right edge
- Two fingers pan and pinch-zoom at any time, drawing mode included
- Finger drawing smoothed so strokes don't look jagged
- Ten-colour palette and a brush-size slider (2–60 px)
- Eraser that removes ink without touching the photo underneath
- Undo / redo, and clear (which keeps the photo)
- Save as a PNG into `Pictures/Sketchpad` in the gallery

### The toolbar

`photo · palette · eraser · laser · pan/zoom` sit on the left of the action bar,
`undo · redo · more` on the right, within reach of a right thumb. The overflow
menu holds background colour, save and clear. The palette button retracts the
colour swatches and the brush slider; the eraser, laser, pan and palette buttons
are toggles and fill in when active.

The laser draws nothing into the document — it is painted after the scene, so it
never reaches an export, the undo history, or the eyedropper. Each sample in the
trail fades on its own age, which is what makes the tail rub itself out a fixed
distance behind the finger rather than all at once when you lift. Eraser and
laser are mutually exclusive: turning one on releases the other. Both take their
colour and width from the palette and the slider.

Sampling a colour renders the scene into a single pixel at the point you tap, so
what you get is exactly what is on screen there — photo, ink or backdrop — with
no separate sampling path to fall out of step with the display.

### How the canvas works

Strokes are stored in *world* coordinates, not screen coordinates, and a single
matrix maps that world onto the display. Ink therefore stays welded to the spot
on the photo where it was drawn, however far you later zoom or pan. With a photo
loaded, one world unit is one photo pixel, so an export is the photo at its own
resolution with the annotations composited on — widened, if you drew past the
edges, to take in that ink too rather than cropping it away.

Photos are decoded down to 3072 px on the long edge (and rotated per their EXIF
tag), which keeps a full-resolution phone photo from costing tens of megabytes
of heap for detail the screen cannot show.

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
│   ├── DrawingView.kt    # world space, pan/zoom, strokes, eraser, undo, export
│   └── MainActivity.kt   # photo picking, palette, mode switch, toolbar, saving
├── app/src/main/res/      # layout, colours, icons, theme
└── app/build.gradle.kts   # SDK 24–35, Kotlin, view binding
```


# Clean HDMI for Sony NEX-5T

A small camera app that gives the **Sony NEX-5T** a clean HDMI output: live view
at 1080 with no menus, icons or black side bar, so you can use it as a webcam or
a video source through a cheap HDMI capture card.

Out of the box the NEX-5T mirrors its own screen to HDMI, overlays and all. Unlike
later Sony bodies it has no "HDMI Info Display: Off" setting. This app runs
on the camera's built-in Android platform (the "PlayMemories Camera Apps" system)
and switches the on-screen graphics layer off while it keeps the live view on HDMI.

It does not modify the firmware. The app gets installed like any other camera app,
and you can quit it at any time.

## Features

- Clean live view on HDMI (the graphics/OSD layer is off)
- Short on-screen overlay (2 s) whenever you change a setting, then the picture is clean again
- Settings from inside the app, whatever the mode dial says:
  - Exposure mode: P / A / S / M
  - Aperture and shutter speed
  - Focus mode (AF / MF), half-press shutter for autofocus
  - Exposure compensation, ISO, white balance
- Lists the codes of unhandled buttons on screen (useful for testing HDMI-CEC remotes)

## Controls

| Button | Action |
|---|---|
| Left / Right | Pick setting (Mode, F, Time, Focus, EV, ISO, WB) |
| Control wheel or Up / Down | Change selected setting |
| Shutter half-press | Autofocus (when Focus is AF) |
| Center | Pin overlay on/off (also shows a yellow debug log) |
| Trash | Exit |

## Before you start

- Set **Aspect Ratio to 16:9** in the normal camera menu. With the default 3:2 the
  live view leaves a black bar on the right of the 16:9 HDMI frame.
- Set **HDMI Resolution** to 1080p (or Auto).
- Connect the HDMI cable before you start the app.

## Install

The camera has no app store anymore. You install the app over USB with
[Sony-PMCA-RE](https://github.com/ma1co/Sony-PMCA-RE):

1. Set the camera's USB connection mode to **Mass Storage** and connect it.
2. Grab `CleanHdmi.apk` from the releases page (or build it, see below).
3. Run:

   ```
   python pmca-console.py install -d native -f CleanHdmi.apk
   ```

   On Windows, `-d native` uses the built-in drivers, so you don't need Zadig.
4. On the camera, open **Application List → Clean HDMI**.

## Build

You need a JDK (8 or newer) and the Android SDK with:

- `platforms/android-35` (only `android.jar` is used, as the boot classpath)
- `build-tools/36.0.0` (`d8`, `aapt2`, `zipalign`, `apksigner`)

The build script expects the SDK at `%LOCALAPPDATA%\Android\Sdk`. Then:

```powershell
cd cleanhdmi
.\build.ps1
```

This produces `cleanhdmi/CleanHdmi.apk`. On the first run the script creates a
local `debug.keystore`.

There is no Gradle. The script calls `javac → d8 → aapt2 → zipalign → apksigner`
directly. Two details matter for the camera's installer:

- The APK uses **v1 (JAR) signing only**, with a SHA1withRSA certificate. If the
  APK carries v2/v3 signatures, the camera rejects it with
  `Communication error 504: Invalid contents for install`.
- The app targets API 10 (Android 2.3, which is what the camera runs).

## How it works

Sony's camera framework ships with the firmware in `/system/framework`. To
compile, the app links against small stub classes in `cleanhdmi/stubs/`, and at
runtime it uses the real ones:

- `com.sony.scalar.hardware.avio.DisplayManager`
  - `switchDisplayOutputTo(DEVICE_ID_HDMI)` routes output to HDMI
  - `controlGraphicsOutputAll(false)` turns off the graphics plane, which leaves only the video plane
- `com.sony.scalar.hardware.CameraEx` handles live view (`getNormalCamera()`),
  aperture and shutter steps, and Sony-specific parameters (ISO) through
  `ParametersModifier`
- `com.sony.scalar.sysutil.ScalarInput` holds the scan codes for the camera's buttons and dials

The firmware research behind this took a few steps. First the NEX-5T firmware
(`Update_NEX5TV101.exe`) was unpacked with fwtool.py. In the Android partition,
`DisplayManager.odex` turned out to expose graphics-plane control. The NEX-5T
framework has no `setOSDOutput`. The stub signatures come from
[OpenMemories-Framework](https://github.com/ma1co/OpenMemories-Framework).

## Compatibility and limitations

- Tested only on a **NEX-5T, firmware 1.01**. Other bodies on the same platform
  (NEX-5R, NEX-6, a5000, a6000 and others) might work, but nobody has tried them.
- The HDMI signal is the camera's live-view feed. It is sharp enough for
  streaming, but it may be upscaled rather than a true full-sensor 1080 readout.
- The app does not take pictures or record video.
- Whether a TV remote can control the app over HDMI-CEC is still untested.
- The camera's power-saving timer still applies. Set it to the longest value.

## Credits

- [ma1co/Sony-PMCA-RE](https://github.com/ma1co/Sony-PMCA-RE) for installing apps over USB
- [ma1co/OpenMemories-Framework](https://github.com/ma1co/OpenMemories-Framework) for the Sony API signatures
- [ma1co/PMCADemo](https://github.com/ma1co/PMCADemo) and BetterManual for reference app patterns
- [ma1co/fwtool.py](https://github.com/ma1co/fwtool.py) for unpacking the firmware

## Disclaimer

This is an independent hobby project. It is not affiliated with or endorsed by
Sony. "Sony", "NEX" and "PlayMemories" are trademarks of Sony Group Corporation.
You install it at your own risk.

## License

[MIT](LICENSE)

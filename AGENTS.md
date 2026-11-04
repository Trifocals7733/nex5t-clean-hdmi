# AGENTS.md

Guidance for coding agents working in this repo.

## What this is

`cleanhdmi/` is an Android 2.3 (API 10) app for the Sony NEX-5T camera platform.
It gives the camera a clean HDMI live view and adds on-screen controls for
exposure and focus. The repo has no Gradle and no tests. There's one activity:
`cleanhdmi/src/se/nex5t/cleanhdmi/MainActivity.java`.

## Layout

- `cleanhdmi/src/`: app code
- `cleanhdmi/stubs/`: **compile-only** stubs of Sony framework classes. They are
  not packaged into the APK (d8 gets them via `--classpath`). Add a method here
  only after you confirm it exists in OpenMemories-Framework's jasmin stubs or in
  the camera's `/system/framework` odex files. A wrong signature compiles fine and
  then crashes on the camera with `NoSuchMethodError`.
- `cleanhdmi/build.ps1`: build pipeline (javac → d8 → aapt2 → zipalign → apksigner)
- Git-ignored local material that may exist on the dev machine:
  - `firmware/`: Sony updater
  - `work/`: the PMCA venv and `FirmwareData.dat`
  - `repos/`: clones of Sony-PMCA-RE, OpenMemories-Framework, PMCADemo, fwtool.py, alpha-fwtools
  - `.codegraph/`

  Never commit any of these. Sony firmware is not redistributable.

## Build and install

```powershell
cd cleanhdmi; .\build.ps1        # -> CleanHdmi.apk
# camera in USB Mass Storage mode:
cd ..\repos\Sony-PMCA-RE
..\..\work\venv\Scripts\python.exe pmca-console.py install -d native -f ..\..\cleanhdmi\CleanHdmi.apk
```

- Bump `android:versionCode`/`versionName` in `cleanhdmi/AndroidManifest.xml` with each release.
- Keep signing **v1 only + SHA1withRSA**. The camera rejects v2/v3-signed APKs with error 504.
- Java source level is 8, but the runtime is Dalvik on Android 2.3. Don't use
  lambdas, `java.util.function`, try-with-resources on non-Closeable types, or any
  Android API above 10.
- Use the `-d native` PMCA driver on Windows. The libusb driver fails with `NotImplementedError`.

## Verifying changes

Nothing runs off-device. A change counts as verified only after someone has
installed it and tested it on the camera with HDMI connected. Say so plainly
when a change is only built and not tested on the device. Errors on the camera
show up in the yellow log, which you open with the center button.

## Code conventions

- Wrap every Sony API call in `try/catch (Throwable)` and send failures to
  `log(...)`. Behaviour differs between firmware versions, and an uncaught
  exception kills the live view.
- Send settings through `camera.createEmptyParameters()` and set only the values
  that changed. White balance is the exception: it needs the full parameter set.
- On-screen text is in Swedish (the owner's language). Code, comments and docs are in English.

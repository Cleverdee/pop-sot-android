# Prince of Persia: Warrior Within for Android

A native Android port of the PC game, made by static recompilation: the game's `pop2.exe` is translated from
x86 to C (Ghidra function list + `translator/x86toc.py`), compiled for arm64, and run on a small Windows API layer
(`host/`): kernel32/user32, DirectInput 8, DirectSound, Bink movies through FFmpeg, and **Direct3D 9 on Vulkan**
(wgpu-native). No emulator, no Wine. It shares its code with the
[Two Thrones port](https://github.com/Cleverdee/pop-t2t-android) (same Jade engine); the game-specific parts are
in `android/profile_pop2.h`.

> **Disclaimer.** This is an independent, non-commercial technical project. It is **not affiliated with, endorsed
> by or connected to Ubisoft**; *Prince of Persia* and *Warrior Within* are trademarks and copyrighted works of
> Ubisoft Entertainment. **This repository contains no game files, no game assets and no code taken from the
> game's executable or data**: the generated C, the executable and the function list are gitignored and never
> published. The app works only with a **genuine copy of the game that you own**, which you copy to your phone
> yourself. Do not share game files. MIT licence, covering only the original code here.

## Playing

1. Install the APK from the [releases](../../releases).
2. Copy your installed PC game folder (GOG: `Prince of Persia Warrior Within`) to the phone, or make a .zip of it.
   The app needs the **unpacked** `pop2.exe`: GOG's is UPX-packed, so run `upx -d pop2.exe` on the PC first.
3. Open the app and pick the folder or the .zip. The files are copied into the app; saves are kept across updates.

Touch controls: a floating stick on the left, the game's buttons on the right (attack, jump, wall run/block,
secondary weapon, rewind, look, camera), swipe the picture to turn the camera, and tap menu items to select them.
A physical controller works too. **Resolution** on the start screen picks *Sharp* (540 lines) or *Fast* (432).

## Building

Windows, PowerShell. The toolchain (Android SDK + NDK r27, JDK 21, SDL2, wgpu-native, Ghidra 12) is the one the
Cricket 07 port fetches; JDK 25 runs Ghidra.

1. `upx -d pop2.exe`, copy it to `..\work\pop2.exe`.
2. Ghidra headless with the scripts in `ghidra/` (`ExportFunctions.java`, plus `NameFromAsserts.java` and
   `RecoverRttiClasses.java` for names) → `..\work\functions.json`.
3. `android\build.ps1` translates (`translator/x86toc.py --all --per-file 300`, then `tools/patch_generated.py`)
   and builds the APK into `android\output\` (first build 20-60 min). `-Install` installs it over USB.

Debug switches go in `Android/data/com.cleverdee.popww/files/env.txt` (one `NAME=value` per line), e.g.
`RC_LOG_FPS=1`, `RC_SHOW_FPS=1`, `RC_GPU_TIMES=1`, `RC_LOG_PAGES=1`; read the log with `adb logcat -s cricket07`.

## Status

Boots, plays the movies (with Skip), creates profiles, menus with tap-to-select, gameplay with touch controls at
about 60 FPS in quiet scenes and 35-50 in big fights (Galaxy A17). Known gaps: the camera doesn't follow the finger
as closely as in the Two Thrones port, tutorial prompts show gamepad button names, and busy fights are CPU-bound.

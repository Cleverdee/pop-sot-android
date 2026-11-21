# Prince of Persia: The Sands of Time for Android

A native Android port of the PC game, made by static recompilation: the game's `POP.EXE` is translated from
x86 to C (Ghidra function list + `translator/x86toc.py`), compiled for arm64, and run on a small Windows API layer
(`host/`): kernel32/user32, DirectInput 8, DirectSound, Bink movies through FFmpeg, and **Direct3D 9 on Vulkan**
(wgpu-native). No emulator, no Wine. It shares its code with the
[Two Thrones](https://github.com/Cleverdee/pop-t2t-android) and
[Warrior Within](https://github.com/Cleverdee/pop-ww-android) ports (same Jade engine family); the game-specific
parts are in `android/profile_pop.h` and `tools/patch_sot.py`.

> **Disclaimer.** This is an independent, non-commercial technical project. It is **not affiliated with, endorsed
> by or connected to Ubisoft**; *Prince of Persia* and *The Sands of Time* are trademarks and copyrighted works of
> Ubisoft Entertainment. **This repository contains no game files, no game assets and no code taken from the
> game's executable or data**: the generated C, the executable and the function list are gitignored and never
> published. The app works only with a **genuine copy of the game that you own**, which you copy to your phone
> yourself. Do not share game files. MIT licence, covering only the original code here.

## Playing

1. Install the APK from the [releases](../../releases).
2. Copy your installed PC game folder (GOG: `Prince of Persia The Sands of Time`, with `POP.EXE`, `prince.bf`,
   `POPData.BF`, `Sound` and `Video`) to the phone, or make a .zip of it. The GOG `POP.EXE` works as installed.
3. Open the app and pick the folder or the .zip. The files are copied into the app; saves are kept across updates.

Touch controls: a floating stick on the left, the game's buttons on the right (attack, jump, wall run/block,
speed kill, rewind, look, camera), swipe the picture to turn the camera, and tap menu items to select them.
**Resolution** on the start screen picks the rendering size.

## Building

Windows, PowerShell. The toolchain (Android SDK + NDK r27, JDK 21, SDL2, wgpu-native, Ghidra 12) is the one the
Cricket 07 port fetches.

1. Copy GOG's `POP.EXE` to `..\work\pop.exe`.
2. Ghidra headless with the scripts in `ghidra/` (`ghidra/export.sh`) → `..\work\functions.json` (~10 min).
3. `android\build.ps1` translates (`translator/x86toc.py --all --per-file 300`, then `tools/patch_sot.py`)
   and builds the APK into `android\output\`. `-Install` installs it over USB.

Debug switches go in `Android/data/com.cleverdee.popsot/files/env.txt` (one `NAME=value` per line), e.g.
`RC_LOG_FPS=1`, `RC_GPU_TIMES=1`, `RC_LOG_PAGES=1`; read the log with `adb logcat -s cricket07`.

## Status

Boots, plays the movies (with Skip), creates profiles, menus with tap-to-select, and gameplay with touch controls
at about 45-60 FPS (Galaxy A17). Known gaps: some enemies are drawn without their legs, and tutorial prompts show
blank key names.

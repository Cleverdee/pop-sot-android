# Prince of Persia: The Sands of Time - Android port hand-over

Tree: `E:\cricket-apk\pop-sot\src`, branch `sot-port` -> remote `cleverdee` main (github.com/Cleverdee/pop-sot-android,
public). Released v0.1.0 on 2026-11-22 (APK work\PrinceOfPersiaSoT-v0.1.0.apk; notes work\release_notes_v0.1.0.md), cloned from the WW tree at v0.1.8
(`ce73c2e`). Package `com.cleverdee.popsot` (Java package/JNI stay `com.cleverdee.pop3`, lib `libpop3.so`).
Work files outside the repo: `E:\cricket-apk\pop-sot\work` (pop.exe = GOG POP.EXE as installed, functions.json,
ghidra.log, translate.log, build.log). Game folder: `E:\cricket-apk\Prince of Persia The Sands of Time`.
Helper scripts outside the repo: `E:\cricket-apk\pop-sot\tools` (xref.py: functions referencing a string/VA).

## Step 0 results (2026-11-21)
- POP.EXE: 32-bit PE, VC7.1 (linker 7.10), entry 0x728559, no relocs, no TLS, no delay imports, not packed.
  `PrinceOfPersia.EXE` (118 KB) is the launcher, not the game.
- No DRM strings (GOG 2012 rebuild). A `P_MessageBoxInsertCD` page exists: watch for a disc check at run time.
- D3D9 (Direct3DCreate9). Imports are a subset of WW's: binkw32 (10), kernel32 (132), user32 (26), gdi32, eax #6,
  dinput8, dsound #11, winmm (waveOutGetDevCapsA, timeSetEvent), advapi32. Static CRT (no msvcr71 import).
- Has 3DNow! code (pfmul/pfadd, untranslated): our CPUID (Intel P6, no MMX/SSE, no 0x80000001) keeps it off.
- Hardware.INI layout differs from WW: per-adapter-format sections + [CAPS] (built-in copy in profile_pop.h).
- Named objects in the exe: POP_Launcher, POP_Game, POP_Watchdog, PrinceSM; `-safe` command line.
- Videos: Video\*.int = plain BIKi (no UBV header). 640x448 logos/intro/loading, 640x346 cut-scenes/visions.
- Game folder extras not needed on the phone: pop_*.zip, manual.pdf (~100 MB).

## Done
- Tree rebranded (gradle id, settings, CMake profile path, build.ps1, launcher text; update repo name
  Cleverdee/pop-sot-android - not created). versionCode 1 / 0.1.0.
- Ported from ac1: translator fixes up to a6efdec (shufps same-register, movnt*, SSE2 doubles, lock prefixes, shared
  bodies for interior pointer entries + `recomp_resume` in runtime), vertex declaration GetDeclaration (host/d3d9.c).
  Not ported: ac1 af897e4 call_indirect inline (needs other ac1 runtime changes).
- `tools/patch_sot.py` replaces patch_generated.py: 800x600/1024x768 defaults -> host size (f_00413cd0, f_00414d40,
  f_00414ee0), 4:3-only Resolutions parser f_00415aa0 (same code as WW's 0x415d30), slider zero-range idiv 0x7182c2.
- profile_pop.h addresses from WW via pop-ww/tools/port_addr.py (unconfirmed): UI pages 0x80bbd8, mouse option
  0x80d08c (alt 0x80d088), add 0x7a7f24 (1.0f), mul 0x7a9b28 (1.1f), pool worker 0x65d4e0 / vtable 0x7af57c.
  Cursor texture fingerprint still 0 (find with RC_LOG_UITEX). GAME_INPLAY_MOVIE_448 0 (host/bink.c switch).
- Ghidra: 9574 functions (~10 min). Translate: 17791 functions, 61 files, max 4.8 MB, 3 min. out_all stamped.

## Now (2026-11-22)
- Open bug to fix next: guards drawn without legs (torso/head fine; the Prince fine). Not the VS constant cap
  (game doesn't read [CAPS]). Capture with files/snap.txt + RC_CAP_RANGE while a guard is on screen.
- Plays: logos -> profile menu -> new profile -> main menu -> Start New Game -> opening cut-scene -> control of
  the Prince in the opening courtyard, 50-60 FPS at 540p. Checked on the phone (A17).
- Controls working (checked): menu/play detection (page stack GAME_UI_VEC 0x10), menu taps select items (cursor
  e416983e), Skip layout during movies, stick walks the Prince (sends W/A/S/D: SoT ignores the pad by default,
  STICK_KEYS in TouchOverlay.java), Jump. Map button removed (no map in SoT). Launcher says The Sands of Time.
- Not yet checked: camera drag (the opening courtyard has a fixed camera), Attack/Wall/Rewind/Look in a fight,
  sound by ear, FPS in later areas.
- Open: tutorial text shows blank key names ("Use Move [, , , ]"); our DirectInput names keys "Key N", so the
  blanks come from elsewhere. "Use safe mode settings?" after a force-stopped run is answered No (harmless).
- Rebranded app UI (2026-11-22): launcher = moonlit night over Azad, hourglass sigil, turquoise logo, sand-gold
  frames, turquoise press glow; overlay gold with turquoise press; icon hourglass recoloured (pop-sot/tools/sot_ui_patch.py
  for icon + overlay colours; Ui.java edited directly). Icon not yet seen on the phone.
- Scripts (pop-sot/tools): rebuild.sh [translate], phone_run.sh, push_game.sh, to_level.sh (profile 2 -> level).

## Next
1. Ask the user before installing/pushing to the phone (other ports share it). Push: POP.EXE, *.BF/prince.bf,
   Sound, Video, Profiles (skip zips/pdf) to Android/data/com.cleverdee.popsot/files/game after first launch.
2. Bring-up: boot -> menus -> first level. Check: which mutex the game wants, InsertCD page, Hardware.INI accepted,
   [dinput] game mouse scale line (confirms mouse address), RC_LOG_PAGES (UI pages), RC_LOG_UITEX (cursor).
3. Camera mapping (does SoT take raw counts like WW? GAME_CAM_RAW_COUNTS / rate band), overlay layout, FPS.

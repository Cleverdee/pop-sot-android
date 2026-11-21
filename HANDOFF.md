# Prince of Persia: The Sands of Time - Android port hand-over

Tree: `E:\cricket-apk\pop-sot\src`, branch `sot-port` (local only, no remote yet), cloned from the WW tree at v0.1.8
(`ce73c2e`). Package `com.cleverdee.popsot` (Java package/JNI stay `com.cleverdee.pop3`, lib `libpop3.so`).
Work files outside the repo: `E:\cricket-apk\pop-sot\work` (pop.exe = GOG POP.EXE as installed, functions.json,
ghidra.log, translate.log). Game folder: `E:\cricket-apk\Prince of Persia The Sands of Time`.

## Step 0 results (2026-11-21)
- POP.EXE: 32-bit PE, VC7.1 (linker 7.10), entry 0x728559, no relocs, no TLS, no delay imports, not packed.
  `PrinceOfPersia.EXE` (118 KB) is the launcher, not the game.
- No DRM strings (GOG 2012 rebuild). A `P_MessageBoxInsertCD` page exists: watch for a disc check at run time.
- D3D9 (Direct3DCreate9). Imports are a subset of WW's: binkw32 (10), kernel32 (132), user32 (26), gdi32, eax #6,
  dinput8, dsound #11, winmm (waveOutGetDevCapsA, timeSetEvent), advapi32. Static CRT (no msvcr71 import).
- Hardware.INI layout differs from WW: per-adapter-format sections + [CAPS] (built-in copy in profile_pop.h).
- Named objects in the exe: POP_Launcher, POP_Game, POP_Watchdog, PrinceSM; `-safe` command line.

## Done
- Tree rebranded (gradle id, settings, CMake profile path, build.ps1, launcher text; update repo name
  Cleverdee/pop-sot-android - not created).
- Ported from ac1: translator fixes up to a6efdec (shufps same-register, movnt*, SSE2 doubles, lock prefixes, shared
  bodies for interior pointer entries + `recomp_resume` in runtime), vertex declaration GetDeclaration (host/d3d9.c).
  Not ported: ac1 af897e4 call_indirect inline (needs other ac1 runtime changes).
- `tools/patch_sot.py` replaces patch_generated.py (WW/T2T address patches don't apply). Only the 800x600/1024x768
  default-size patch, printing each hit.
- profile_pop.h: game addresses (UI pages, mouse option, pool, cursor) are 0 = features off until found.
  dinput.c no longer writes GAME_MOUSE_OPT when it is 0.
- Ghidra export: 9574 functions, 525 jump tables (~10 min).

## Next
1. Translate (running/see work\translate.log), check file sizes + unsupported mnemonics, stamp out_all\.exe-hash,
   run patch_sot.py, build APK (`powershell -ExecutionPolicy Bypass -File android\build.ps1`).
2. Ask the user before installing/pushing to the phone (other ports share it).
3. Bring-up: boot -> menus -> first level. Then find SoT addresses (UI pages, cursor texture, mouse option, camera)
   with pop-ww/tools/port_addr.py, overlay layout, FPS.

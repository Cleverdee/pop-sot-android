/* Per-game settings for the Windows layer: Prince of Persia: The Sands of Time (GOG POP.EXE, VC7.1, 2003 Jade engine).
 * Same import set as Warrior Within (checked 2026-11-21): binkw32, dinput8, dsound #11, eax #6, d3d9, winmm.
 */
#ifndef RC_PROFILE_H
#define RC_PROFILE_H

#define PROFILE_EXE        "POP.EXE"      /* the executable, relative to the game folder */
#define PROFILE_GUEST_ROOT "C:\\Ubisoft\\Prince of Persia"   /* the install folder, as the game sees it */
#define PROFILE_GUEST_EXE  "POP.EXE"      /* the executable's name, as the game sees it */
#define PROFILE_TITLE      "Prince of Persia: The Sands of Time"   /* native window title */

/* Extra registry values, appended to the built-in ones in host/misc.c */
#define PROFILE_REGISTRY

/* Named mutexes that already exist when the game starts (as if PrinceOfPersia.EXE, the launcher, had made them).
   The exe names "POP_Launcher", "POP_Game", "POP_Watchdog" (strings at 0x7a731c..); which one it checks is
   confirmed at run time. */
#define PROFILE_EXISTING_MUTEXES "POP_Launcher",

/* Hardware.INI is written by the launcher's hardware test. SoT's layout (unlike WW's) has one section per adapter
   format and a [CAPS] section; values below follow the PC file, with the host's display modes. */
#define PROFILE_BUILTIN_INIS \
    { "Hardware.INI", \
      "[CONFIG]\n" \
      "Version=1\n" \
      "DontDisplayErrors=1\n" \
      "[VIDEO]\n" \
      "Description=Android\n" \
      "Resolutions=640,480,32, 800,600,32, 1024,768,32, 1280,720,32, 1280,960,32, 1280,1024,32, 1600,1200,32\n" \
      "AdapterFormats=D3DFMT_X8R8G8B8\n" \
      "[D3DFMT_X8R8G8B8]\n" \
      "TextureFormats=D3DFMT_A8R8G8B8, D3DFMT_X8R8G8B8, D3DFMT_R5G6B5, D3DFMT_A1R5G5B5, D3DFMT_X1R5G5B5, D3DFMT_P8, " \
      "D3DFMT_DXT1, D3DFMT_DXT3, D3DFMT_DXT5\n" \
      "RenderTargets=D3DFMT_A8R8G8B8, D3DFMT_R5G6B5\n" \
      "BufferDepths=D3DFMT_D24S8, D3DFMT_D24X8, D3DFMT_D16\n" \
      "[CAPS]\n" \
      "NonPow2Textures=0\nStreamsCount=16\nPureDevice=1\nWFog=1\nZFog=1\nPixelFog=1\nFogRange=1\nVertexFog=1\n" \
      "ZBias=0\nSquareOnlyTextures=0\nCanStretchRect=1\nAlphaPalette=1\nMaxVertexShaderConst=256\n" \
      "FullscreenGamma=1\nAllowDisableVSync=1\nShadowScaling=1\nColorDiffusion=1\nForceVSFog=1\nInvertFogRange=0\n" \
      "ZEnableFix=0\n" },

/* Game addresses the host reads, mapped from the WW ones with pop-ww/tools/port_addr.py (2026-11-21; votes in
   brackets). Mouse add/mul hold 1.0f / 1.1f as in WW. Not confirmed at run time yet. 0 = not found (feature off). */
#define GAME_UI_PAGES     0x0080bbd8u   /* [15 of 18]; confirmed by FUN_00409870 (opens a page: pushes onto [0x80bbd8]+0xc) */
/* SoT's page stack is the object at +0xc: +0x10 begin, +0x14 end (8-byte {page, flag} entries), +0x1c count
   (FUN_00712440 top, FUN_00712a70 push). T2T/WW keep begin/end at +0x14/+0x18. */
#define GAME_UI_VEC       0x10
#define GAME_MOUSE_OPT    0x0080d08cu   /* [15; 0x80d088 had 6] */
#define GAME_MOUSE_ADD    0x007a7f24u
#define GAME_MOUSE_MUL    0x007a9b28u
#define GAME_POOL_WORKER  0x0065d4e0u   /* sound I/O pool worker (thread start), vtable below */
#define GAME_POOL_VTABLE  0x007af57cu
/* SoT movies (Video\*.int, plain BIKi, no UBV header): logos/intro/loading 640x448, cut-scenes and visions 640x346.
   None plays inside play like WW's map/power-up movies, so 640x448 counts as a cut-scene (Skip layout). */
#define GAME_INPLAY_MOVIE_448 0
/* the dagger cursor: 128x128, drawn with a second 128x128 (35be3968) at the same spot (RC_LOG_UITEX on the profile
   menu, 2026-11-22); tip near its top-left. Unconfirmed: check menu taps land. */
#define GAME_CURSOR_TEX   0xe416983eu
#define GAME_CURSOR_HOT_X 4.f
#define GAME_CURSOR_HOT_Y 4.f

#endif

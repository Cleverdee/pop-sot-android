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

/* Game addresses the host reads. 0 = not found in SoT yet: the host features using them stay off (no UI page
   list -> no menu/play detection; no mouse option -> camera without the game's mouse scale; no pool nudge).
   To find: tools/port_addr.py (outer pop-ww/tools) from the WW values, then RC_LOG_PAGES / RC_LOG_UITEX. */
#define GAME_UI_PAGES     0u
#define GAME_MOUSE_OPT    0u
#define GAME_MOUSE_ADD    0u
#define GAME_MOUSE_MUL    0u
#define GAME_POOL_WORKER  0u
#define GAME_POOL_VTABLE  0u
#define GAME_CURSOR_TEX   0u
#define GAME_CURSOR_HOT_X 2.f
#define GAME_CURSOR_HOT_Y 3.f

#endif

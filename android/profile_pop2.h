/* Per-game settings for the Windows layer: where the game believes it is installed and what it finds
 * in the registry. This default suits a game that only needs its own folder. To use your own, copy
 * this file and build with PROFILE=/path/to/your_profile.h (see build.sh).
 */
#ifndef RC_PROFILE_H
#define RC_PROFILE_H

#define PROFILE_EXE        "POP2.EXE"     /* the executable, relative to the game folder */
#define PROFILE_GUEST_ROOT "C:\\Ubisoft\\POP2"     /* the install folder, as the game sees it */
#define PROFILE_GUEST_EXE  "POP2.EXE"     /* the executable's name, as the game sees it */
#define PROFILE_TITLE      "Prince of Persia: Warrior Within"         /* native window title */

/* Extra registry values, appended to the built-in ones in host/misc.c:
 *   { key, value, type (1 = REG_SZ, 4 = REG_DWORD), string data, dword data },
 * e.g. { "SOFTWARE\\Vendor\\Game", "InstallDir", 1, PROFILE_GUEST_ROOT, 0 }, */
#define PROFILE_REGISTRY

/* Named mutexes that already exist when the game starts (as if a launcher had made them): "name", */
#define PROFILE_EXISTING_MUTEXES "POP5Launcher",

/* Hardware.INI is written by the launcher's hardware test: the display modes and formats the game may use */
#define PROFILE_BUILTIN_INIS \
    { "Hardware.INI", \
      "[VIDEO]\n" \
      "Resolutions=640,480,32,800,600,32,1024,768,32,1280,720,32,1280,960,32,1280,1024,32,1600,1200,32\n" \
      "AdapterFormats=D3DFMT_X8R8G8B8\n" \
      "TextureFormats=D3DFMT_A8R8G8B8,D3DFMT_X8R8G8B8,D3DFMT_R5G6B5,D3DFMT_A1R5G5B5,D3DFMT_X1R5G5B5,D3DFMT_P8," \
      "D3DFMT_DXT1,D3DFMT_DXT3,D3DFMT_DXT5\n" \
      "RenderTargets=D3DFMT_A8R8G8B8,D3DFMT_X8R8G8B8\n" \
      "BufferDepths=D3DFMT_D24S8,D3DFMT_D24X8,D3DFMT_D16,D3DFMT_D32\n" },

/* Game addresses the host reads (found from the Two Thrones ones with tools/port_addr.py):
 *   UI page list pointer (host/gamephase.c), Mouse speed option and its addend/multiplier (host/dinput.c),
 *   the sound engine's I/O pool worker and vtable (host/kernel32.c pool nudge) */
#define GAME_UI_PAGES     0x00da9204u
#define GAME_MOUSE_OPT    0x0093f074u
#define GAME_MOUSE_ADD    0x008bdf44u
#define GAME_MOUSE_MUL    0x008bf2c8u
#define GAME_POOL_WORKER  0x00741930u
#define GAME_POOL_VTABLE  0x008c8c2cu
#define GAME_CURSOR_TEX   0x6c143e60u   /* the quill cursor (64x64; RC_LOG_UITEX) */
#define GAME_CURSOR_HOT_X 6.f
#define GAME_CURSOR_HOT_Y 4.f
/* Warrior Within adds raw mouse counts to the camera (x -= dx, y += dy; FUN_00422d..., near 0x422ee2): the
   (Mouse speed + 1) * 1.1 scale only moves the cursor. Two Thrones scales both. Same x sign as Two Thrones. */
#define GAME_CAM_RAW_COUNTS 1

#endif

/* Prince of Persia: The Two Thrones: imports with no real implementation yet.
 * EAX is absent (Direct3D 9: d3d9.c).
 */
#include "win32.h"

#define W(name) static void w_##name(CPU *c)

W(ShellExecuteA) { STUB("ShellExecuteA(%s)\n", ARG(2) ? GSTR(ARG(2)) : ""); RET(6, 33); }
W(waveOutGetDevCapsA) { RET(3, 2 /* MMSYSERR_BADDEVICEID */); }
/* GOG's amir.dll replaces StarForce; the game never calls SF_Init (the import only makes Windows load the DLL). */
W(SF_Init) { STUB("SF_Init\n"); RET_CDECL(1); }

void w_DirectInput8Create(CPU *c);  /* dinput.c */

/* Bink: host/bink.c */
void bink_BinkOpen(CPU *c), bink_BinkClose(CPU *c), bink_BinkDoFrame(CPU *c), bink_BinkNextFrame(CPU *c),
    bink_BinkWait(CPU *c), bink_BinkGoto(CPU *c), bink_BinkPause(CPU *c), bink_BinkSetVolume(CPU *c),
    bink_BinkSetSoundTrack(CPU *c), bink_BinkSetSoundSystem(CPU *c), bink_BinkOpenDirectSound(CPU *c),
    bink_BinkCopyToBuffer(CPU *c);

#define B(name, n) { "BINKW32.DLL!_" #name "@" #n, bink_##name }
static const ImportHandler list[] = {
    { "SHELL32.DLL!ShellExecuteA", w_ShellExecuteA },
    { "WINMM.DLL!waveOutGetDevCapsA", w_waveOutGetDevCapsA },
    { "EAX.DLL!#6", NULL },      /* EAXDirectSoundCreate(guid, &ds, outer): same as DirectSoundCreate; patched below */
    { "DINPUT8.DLL!DirectInput8Create", w_DirectInput8Create },
    { "DSOUND.DLL!#11", NULL },  /* DirectSoundCreate8; patched below */
    /* GOG build: the same imports by name instead of ordinal, plus GOG's StarForce replacement (amir.dll). */
    { "EAX.DLL!EAXDirectSoundCreate8", NULL },
    { "DSOUND.DLL!DirectSoundCreate8", NULL },
    { "AMIR.DLL!SF_Init", w_SF_Init },
    B(BinkOpen, 8), B(BinkClose, 4), B(BinkDoFrame, 4), B(BinkNextFrame, 4), B(BinkWait, 4), B(BinkGoto, 12),
    B(BinkPause, 8), B(BinkSetVolume, 12), B(BinkSetSoundTrack, 8), B(BinkSetSoundSystem, 8),
    B(BinkOpenDirectSound, 4), B(BinkCopyToBuffer, 28),
};
void w_DirectSoundCreate(CPU *c);  /* dsound.c */
static ImportHandler g_list[sizeof list / sizeof list[0]];
static ImportTable make(void) {
    for (size_t i = 0; i < sizeof list / sizeof list[0]; i++) {
        g_list[i] = list[i];
        if (!g_list[i].fn) g_list[i].fn = w_DirectSoundCreate;
    }
    return (ImportTable){ g_list, sizeof list / sizeof list[0] };
}
const ImportTable *pop3_imports(void) { static ImportTable t; if (!t.list) t = make(); return &t; }

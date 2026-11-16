/* Hooks android/host/window_android.c (shared with the Cricket 07 port) calls that Two Thrones doesn't have:
 * multiplayer (host/netplay.c). The game phase for the touch layouts is in host/gamephase.c. */
#include <stdint.h>

int netplay_on(void) { return 0; }
uint32_t netplay_status(int what) { (void)what; return 0; }
/* the overlay's Screen setting (4:3 / stretch / wide): Two Thrones' renderer letterboxes to the game's own aspect
   (16:9 at the 1080p the build patches in), so the setting does nothing yet */
void render_set_aspect(int mode) { (void)mode; }

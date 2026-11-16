/* Debugging: a Cheat-Engine-style search of the game's memory for float values, driven from a request file
 * (<game>/../scan.txt, one command, deleted when read), checked once per frame on the game thread:
 *   init            remember every float slot in the heap and VirtualAlloc'd memory
 *   changed|same|inc|dec   keep the slots that changed / stayed / grew / shrank since the last command
 *   near <v> <tol>  keep the slots now within tol of v
 *   list            log up to 300 surviving slots (address, value) as [scan]
 *   poke <addr> <v> write a float (hex address)
 * Used to find Two Thrones' camera angles (host/gamephase.c drives them from touch). */
#include "win32.h"
#include <math.h>

static uint8_t *g_prev, *g_keep;   /* previous contents; one bit per 4-byte slot */
static uint32_t g_lo[3], g_hi[3], g_off[3], g_total;

static float slot(uint32_t i, int prev) {   /* slot i (over both ranges): its value now or before */
    float v;
    uint32_t r = i * 4 < g_off[1] ? 0 : i * 4 < g_off[2] ? 1 : 2, a = g_lo[r] + (i * 4 - g_off[r]);
    if (prev) memcpy(&v, g_prev + i * 4, 4); else memcpy(&v, HP(a), 4);
    return v;
}
static uint32_t slot_addr(uint32_t i) { uint32_t r = i * 4 < g_off[1] ? 0 : i * 4 < g_off[2] ? 1 : 2; return g_lo[r] + (i * 4 - g_off[r]); }

static void snapshot(void) {
    for (int r = 0; r < 3; r++) memcpy(g_prev + g_off[r], HP(g_lo[r]), g_hi[r] - g_lo[r]);
}

static void run(const char *cmd) {
    char op[16] = "";
    float a = 0, b = 0;
    uint32_t addr = 0;
    sscanf(cmd, "%15s", op);
    if (!strcmp(op, "init")) {
        extern void guest_mem_ranges(uint32_t r[4]);
        uint32_t r[4];
        guest_mem_ranges(r);
        g_lo[0] = r[0]; g_hi[0] = r[1]; g_lo[1] = r[2]; g_hi[1] = r[3];
        g_lo[2] = 0x9d8000u; g_hi[2] = 0xe87000u;   /* the executable's data section: the script engine keeps state there */
        g_off[0] = 0; g_off[1] = g_hi[0] - g_lo[0]; g_off[2] = g_off[1] + g_hi[1] - g_lo[1];
        g_total = (g_off[2] + g_hi[2] - g_lo[2]) / 4;
        free(g_prev); free(g_keep);
        g_prev = malloc((size_t)g_total * 4); g_keep = malloc(g_total / 8 + 1);
        if (!g_prev || !g_keep) { fprintf(stderr, "[scan] out of memory\n"); g_total = 0; return; }
        snapshot();
        uint32_t n = 0;
        for (uint32_t i = 0; i < g_total; i++) {
            float v = slot(i, 1);
            int ok = isfinite(v) && fabsf(v) < 1e5f && fabsf(v) > 1e-6f;
            if (ok) { g_keep[i / 8] |= (uint8_t)(1 << (i & 7)); n++; } else g_keep[i / 8] &= (uint8_t)~(1 << (i & 7));
        }
        fprintf(stderr, "[scan] init: %u MB, %u float slots\n", g_total >> 18, n);
        return;
    }
    if (!strcmp(op, "poke")) {   /* poke <addr> <v> [<addr> <v> ...]: all in the same frame */
        const char *p = cmd + 4;
        int used;
        while (sscanf(p, " %x %f%n", &addr, &a, &used) == 2) {
            memcpy(HP(addr), &a, 4);
            fprintf(stderr, "[scan] poke %08x = %g\n", addr, a);
            p += used;
        }
        return;
    }
    if (!strcmp(op, "peek")) {   /* peek <addr> <n>: n floats */
        int n = 16;
        sscanf(cmd, "%*s %x %d", &addr, &n);
        for (int i = 0; i < n && i < 256; i++) {
            float v;
            memcpy(&v, HP(addr + 4 * i), 4);
            fprintf(stderr, "%s %08x=%g", i % 8 ? "" : (i ? "\n[peek]" : "[peek]"), addr + 4 * i, v);
        }
        fprintf(stderr, "\n");
        return;
    }
    if (!g_total) return;
    if (!strcmp(op, "keep")) {   /* keep <lo> <hi>: only slots in that address range */
        uint32_t lo = 0, hi = 0, n = 0;
        sscanf(cmd, "%*s %x %x", &lo, &hi);
        for (uint32_t i = 0; i < g_total; i++)
            if (g_keep[i / 8] >> (i & 7) & 1) {
                uint32_t ad = slot_addr(i);
                if (ad >= lo && ad < hi) n++; else g_keep[i / 8] &= (uint8_t)~(1 << (i & 7));
            }
        fprintf(stderr, "[scan] %s: %u left\n", cmd, n);
        return;
    }
    if (!strcmp(op, "list")) {
        uint32_t n = 0, shown = 0;
        for (uint32_t i = 0; i < g_total; i++) if (g_keep[i / 8] >> (i & 7) & 1) n++;
        fprintf(stderr, "[scan] %u left:", n);
        for (uint32_t i = 0; i < g_total && shown < 300; i++)
            if (g_keep[i / 8] >> (i & 7) & 1) {   /* short lines: the Android log file drops lines over 2 KB */
                fprintf(stderr, "%s %08x=%g", shown % 15 ? "" : "\n[scan]", slot_addr(i), slot(i, 0));
                shown++;
            }
        fprintf(stderr, "\n");
        return;
    }
    int mode = !strcmp(op, "changed") ? 1 : !strcmp(op, "same") ? 2 : !strcmp(op, "inc") ? 3 : !strcmp(op, "dec") ? 4
             : !strcmp(op, "near") ? 5 : 0;
    if (!mode) { fprintf(stderr, "[scan] unknown command %s\n", cmd); return; }
    if (mode == 5) sscanf(cmd, "%*s %f %f", &a, &b);
    uint32_t n = 0;
    for (uint32_t i = 0; i < g_total; i++) {
        if (!(g_keep[i / 8] >> (i & 7) & 1)) continue;
        float p = slot(i, 1), v = slot(i, 0);
        int ok = isfinite(v) && (mode == 1 ? fabsf(v - p) > 1e-5f * (1 + fabsf(p)) : mode == 2 ? v == p : mode == 3 ? v > p
                                 : mode == 4 ? v < p : fabsf(v - a) <= b);
        if (ok) n++; else g_keep[i / 8] &= (uint8_t)~(1 << (i & 7));
    }
    snapshot();
    fprintf(stderr, "[scan] %s: %u left\n", cmd, n);
}

void memscan_frame(void) {
    static double last;
    if (host_seconds() - last < 0.3) return;
    last = host_seconds();
    char path[4200], cmd[128];
    snprintf(path, sizeof path, "%s/../scan.txt", g_game_root);
    FILE *f = fopen(path, "r");
    if (!f) return;
    if (!fgets(cmd, sizeof cmd, f)) cmd[0] = 0;
    fclose(f);
    remove(path);
    cmd[strcspn(cmd, "\r\n")] = 0;
    run(cmd);
}

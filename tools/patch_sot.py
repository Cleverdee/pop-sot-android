#!/usr/bin/env python3
"""Source patches applied to the translator's output for Prince of Persia: The Sands of Time (build.ps1 runs it).

The Warrior Within / Two Thrones patches (tools/patch_generated.py) name WW/T2T addresses; SoT gets its own list here,
each patch added once a run shows it is needed.

- Video size defaults: width/height pairs of immediate stores (800x600, 1024x768) take the host size instead (same idea
  as patch_generated.py). Every patched function is printed so a hit in unrelated code is easy to spot.
Usage: patch_sot.py <out dir>
"""
import glob
import re
import sys


def pair_re(w, h):
    return re.compile(
        r"(W32\(\(uint32_t\)\([^;\n]*\), \(uint32_t\)\(\(uint32_t\))0x%xu"
        r"(\)\); /\* mov dword ptr \[[^\]\n]*\], 0x%x \*/\n\s*W32\(\(uint32_t\)\([^;\n]*\), \(uint32_t\)\(\(uint32_t\))0x%xu"
        r"(\)\); /\* mov dword ptr \[[^\]\n]*\], 0x%x \*/)" % (w, w, h, h))


def func_at(s, pos):
    i = s.rfind("\nvoid f_", 0, pos)
    return s[i + 6:i + 16] if i >= 0 else "?"


done = 0
for w, h in ((0x320, 0x258), (0x400, 0x300)):
    PAIR = pair_re(w, h)
    for f in sorted(glob.glob(sys.argv[1] + "/*.c")):
        s = open(f).read()
        hits = [func_at(s, m.start()) for m in PAIR.finditer(s)]
        if not hits:
            continue
        s2 = PAIR.sub(lambda m: m.group(1) + "({ extern uint32_t host_video_width(void); host_video_width(); })" + m.group(2)
                      + "({ extern uint32_t host_video_height(void); host_video_height(); })" + m.group(3), s)
        open(f, "w").write(s2)
        done += len(hits)
        print("  %dx%d default -> host size in %s" % (w, h, ", ".join(hits)))

print(f"patch_sot: {done} patch(es)")

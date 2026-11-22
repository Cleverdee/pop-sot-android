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

# The Hardware.INI Resolutions parser (FUN_00415aa0, WW's FUN_00415d30) keeps only 4:3 modes (640x480 ... 2048x1536):
# a phone-shaped host mode would be dropped and the game would quit (FindAvailableDisplayMode). The 640x480 slot takes
# the host size, as in WW.
for f in glob.glob(sys.argv[1] + "/*.c"):
    s = open(f).read()
    if "void f_00415aa0(" not in s:
        continue
    i = s.index("void f_00415aa0(")
    j = s.index("\n}\n", i)
    body = s[i:j]
    b2 = body.replace("(uint32_t)0x280u); /* cmp eax, 0x280 */",
                      "({ extern uint32_t host_video_width(void); host_video_width(); })); /* cmp eax, 0x280 (host size) */") \
             .replace("(uint32_t)0x1e0u); /* cmp dword ptr [esp + 0x1c], 0x1e0 */",
                      "({ extern uint32_t host_video_height(void); host_video_height(); })); /* cmp dword ptr [esp + 0x1c], 0x1e0 (host size) */")
    if b2 != body and "cmp eax, 0x320" in body:
        open(f, "w").write(s[:i] + b2 + s[j:])
        done += 2
        print("  Resolutions parser f_00415aa0: 640x480 slot -> host size")
    else:
        print("  WARNING: Resolutions parser f_00415aa0 not patched (code changed?)")

# The menu slider draw code (FUN_00718060, the same code as WW 0x8355d0) places the knob at
# (value - min) * width / (max - min); the host's single resolution gives max == min and the idiv traps when the
# Graphics page opens. A zero range puts the knob at the start.
for f in glob.glob(sys.argv[1] + "/*.c"):
    s = open(f).read()
    s2 = s
    for addr in ("0x007182c2u",):
        trap = 'guest_trap(c, %s, "divide by zero"); }' % addr
        k = s2.find(trap)
        if k < 0 or "imul eax, ebx" not in s2[k - 2000:k]:
            continue
        a = s2.rfind("if (!d_) {", 0, k)
        s2 = s2[:a] + "if (!d_) { r->eax = 0; r->edx = 0; } else {" + s2[k + len(trap):]
        e = s2.find("} /* idiv dword ptr [esp + 0x18] */", a)
        s2 = s2[:e] + "} } /* idiv dword ptr [esp + 0x18] (zero range -> 0) */" + s2[e + len("} /* idiv dword ptr [esp + 0x18] */"):]
        done += 1
        print("  slider idiv 0x7182c2: zero range -> 0")
    if s2 != s:
        open(f, "w").write(s2)

# SoT is built with asserts: the slider's SetRange (FUN_00717850) asserts min < max (MNU_Slider.cpp line 40) and the
# Graphics page sets the Resolution slider to (0, modes - 1) = (0, 0) with the host's one mode -> "Runtime Error!" abort.
# Equal min and max are let through (jl -> jle). The slider draw's float paths (fidiv by the range) give 0 for a zero
# range, like the idiv above.
for f in glob.glob(sys.argv[1] + "/*.c"):
    s = open(f).read()
    if "void f_00717850(" not in s:
        continue
    i = s.index("void f_00717850(")
    j = s.index("\n}\n", i)
    body = s[i:j]
    old = "if ((r->sf != r->of)) goto L_00717875; /* jl 0x717875 */"
    if old in body:
        body = body.replace(old, "if ((r->sf != r->of) || r->zf) goto L_00717875; /* jl 0x717875 (min == max allowed) */")
        s = s[:i] + body + s[j:]
        done += 1
        print("  slider SetRange 0x717850: min == max allowed")
    else:
        print("  WARNING: slider SetRange 0x717850 not patched (code changed?)")
    open(f, "w").write(s)
for f in glob.glob(sys.argv[1] + "/*.c"):
    s = open(f).read()
    if "void f_00718060(" not in s:
        continue
    i = s.index("void f_00718060(")
    j = s.index("\n}\n", i)
    body = s[i:j]
    old = "FDIVx(x, x->s[0], (double)(int32_t)R32((uint32_t)(r->esp + 0x18u)))"
    new = ("((int32_t)R32((uint32_t)(r->esp + 0x18u)) ? FDIVx(x, x->s[0], (double)(int32_t)R32((uint32_t)(r->esp + 0x18u)))"
           " : 0.0) /* zero range -> 0 */")
    n = body.count(old)
    if n:
        s = s[:i] + body.replace(old, new) + s[j:]
        open(f, "w").write(s)
        done += n
        print("  slider draw 0x718060: %d fidiv(s) zero range -> 0" % n)
    else:
        print("  WARNING: slider draw fidiv not patched (code changed?)")

print(f"patch_sot: {done} patch(es)")

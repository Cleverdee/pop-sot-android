/* Direct3D 8 vertex shaders (vs.1.1) and vertex declarations (Direct3D 9 declarations arrive converted to
 * Direct3D 8 tokens by d3d9.c, with the shader's dcl registers already applied).
 *
 * The declaration says how stream data maps onto input registers v0-v15, the shader runs per vertex,
 * and its outputs (oPos in clip space, oD0/oD1, oT0-oT7) feed the fixed-function pixel stage. Each
 * shader is decoded once into a compact instruction list, which either runs here on the CPU
 * (vs_run_batch, four vertices at a time) or is translated to a WGSL vertex function (vs_to_wgsl) so
 * the GPU runs it; then the CPU only fetches the input registers (vs_fetch_n), expanding packed
 * vertex types (SHORT4N, UBYTE4, FLOAT16 ...) to float4. */
#include "vshader.h"

#include <math.h>
#include <stdarg.h>
#include <stdlib.h>

enum { RT_TEMP = 0, RT_INPUT = 1, RT_CONST = 2, RT_ADDR = 3, RT_RASTOUT = 4, RT_ATTROUT = 5, RT_TEXOUT = 6 };

typedef struct { uint8_t type, rel, neg, swz[4], ident; uint16_t num; } Src;   /* ident: no swizzle, no negation */
typedef struct { uint16_t op; uint8_t dtype, dmask; uint16_t dnum; uint8_t nsrc; Src s[3]; } Insn;

typedef struct {
    Insn *code; uint32_t n;
    /* declaration: input register sources */
    struct { uint8_t used, stream, type; uint16_t offset; } in[16];
    uint8_t used_in[16]; int nused;                /* the used input registers, in order (the GPU vertex layout) */
    float defs[96][4]; uint8_t has_def[96];        /* constants from the declaration and `def` */
    int any_def;
    int fixed_function;                            /* declaration without a program */
    int ok;
} VsProgram;

/* operand count (destination + sources) per vs.1.1 opcode */
static int nparams(uint32_t op) {
    switch (op) {
    case 0: return 0;
    case 1: case 6: case 7: case 14: case 15: case 16: case 19: case 78: case 79: return 2;
    case 2: case 3: case 5: case 8: case 9: case 10: case 11: case 12: case 13: case 17:
    case 20: case 21: case 22: case 23: case 24: return 3;
    case 4: case 18: return 4;
    default: return -1;
    }
}

static void parse_decl(VsProgram *p, const uint32_t *t, uint32_t n) {
    uint32_t stream = 0, offset = 0;
    for (uint32_t i = 0; i < n; i++) {
        uint32_t tok = t[i];
        if (tok == 0xFFFFFFFFu) break;
        switch (tok >> 29) {
        case 1: stream = tok & 0xF; offset = 0; break;               /* D3DVSD_STREAM */
        case 2:
            if (tok & 0x10000000u) offset += 4 * ((tok >> 16) & 0xF);   /* D3DVSD_SKIP */
            else {
                uint32_t reg = tok & 0x1F, type = (tok >> 16) & 0x1F;
                if (reg < 16) { p->in[reg].used = 1; p->in[reg].stream = (uint8_t)stream; p->in[reg].type = (uint8_t)type;
                                p->in[reg].offset = (uint16_t)offset; }
                static const uint8_t sz[17] = { 4, 8, 12, 16, 4, 4, 4, 8, 4, 4, 8, 4, 8, 4, 4, 4, 8 };   /* D3DDECLTYPE sizes */
                offset += type < 17 ? sz[type] : 4;
            }
            break;
        case 4: {                                                    /* D3DVSD_CONST */
            uint32_t cnt = (tok >> 25) & 0xF, addr = tok & 0x7F;
            for (uint32_t k = 0; k < cnt && i + 4 <= n; k++, addr++) {
                if (addr < 96) { memcpy(p->defs[addr], &t[i + 1], 16); p->has_def[addr] = 1; }
                i += 4;
            }
            break;
        }
        case 5: i += (tok >> 24) & 0x1F; break;                     /* D3DVSD_EXT */
        default: break;
        }
    }
}

static Src decode_src(uint32_t t) {
    Src s;
    s.type = (uint8_t)((t >> 28) & 7);
    s.num = (uint16_t)(t & 0x1FFF);
    s.rel = (t >> 13) & 1;
    s.neg = ((t >> 24) & 0xF) == 1;
    for (int k = 0; k < 4; k++) s.swz[k] = (uint8_t)((t >> (16 + 2 * k)) & 3);
    s.ident = !s.neg && s.swz[0] == 0 && s.swz[1] == 1 && s.swz[2] == 2 && s.swz[3] == 3;
    return s;
}

static int parse_code(VsProgram *p, const uint32_t *t, uint32_t n) {
    if (!n || (t[0] & 0xFFFF0000u) != 0xFFFE0000u) return 0;
    p->code = calloc(n, sizeof(Insn));
    for (uint32_t i = 1; i < n;) {
        uint32_t tok = t[i++], op = tok & 0xFFFF;
        if (tok == 0x0000FFFFu) break;
        if (op == 0xFFFE) { i += (tok >> 16) & 0x7FFF; continue; }   /* comment */
        if (op == 31) { i += 2; continue; }                         /* dcl_usage v#: the declaration is bound by d3d9.c */
        if (op == 81) {                                             /* def c#, f, f, f, f */
            uint32_t r = t[i] & 0x1FFF;
            if (r < 96) { memcpy(p->defs[r], &t[i + 1], 16); p->has_def[r] = 1; }
            i += 5;
            continue;
        }
        int np = nparams(op);
        if (np < 0) { fprintf(stderr, "[vs] unsupported opcode %u\n", op); return 0; }
        if (np == 0) continue;
        Insn *in = &p->code[p->n++];
        in->op = (uint16_t)op;
        uint32_t d = t[i++];
        in->dtype = (uint8_t)((d >> 28) & 7);
        in->dnum = (uint16_t)(d & 0x1FFF);
        in->dmask = (uint8_t)((d >> 16) & 0xF);
        in->nsrc = (uint8_t)(np - 1);
        for (int k = 0; k < np - 1; k++) {
            uint32_t s = t[i++];
            in->s[k] = decode_src(s);
            if (in->s[k].rel && (t[0] & 0xFFFF) >= 0x0200) i++;     /* vs.2.0 relative token (not expected) */
        }
    }
    return 1;
}

static VsProgram *program_for(Shader *sh) {
    if (sh->backend) return sh->backend;
    VsProgram *p = calloc(1, sizeof *p);
    parse_decl(p, sh->decl, sh->decl_len);
    if (!sh->code) { p->fixed_function = 1; p->ok = 1; }
    else p->ok = parse_code(p, sh->code, sh->code_len);
    if (!p->ok) fprintf(stderr, "[vs] could not decode shader (version %08x)\n", sh->code ? sh->code[0] : 0);
    if (getenv("RC_DUMP_VS")) {   /* raw tokens, for offline disassembly */
        fprintf(stderr, "[vsdump %x] decl", sh->handle);
        for (uint32_t i = 0; i < sh->decl_len; i++) fprintf(stderr, " %08x", sh->decl[i]);
        fprintf(stderr, "\n[vsdump %x] code", sh->handle);
        for (uint32_t i = 0; i < sh->code_len; i++) fprintf(stderr, " %08x", sh->code[i]);
        fprintf(stderr, "\n");
    }
    for (int i = 0; i < 96; i++) if (p->has_def[i]) p->any_def = 1;
    for (int i = 0; i < 16; i++) if (p->in[i].used) p->used_in[p->nused++] = (uint8_t)i;
    sh->backend = p;
    return p;
}

int vs_is_fixed_function(Shader *sh) { return program_for(sh)->fixed_function; }

/* ------------------------------------------------------------------ input fetch */

static void fetch(const uint8_t *src, uint32_t type, float *o) {
    o[0] = o[1] = o[2] = 0; o[3] = 1;
    switch (type) {
    case 0: memcpy(o, src, 4); break;
    case 1: memcpy(o, src, 8); break;
    case 2: memcpy(o, src, 12); break;
    case 3: memcpy(o, src, 16); break;
    case 4: o[0] = src[2] / 255.f; o[1] = src[1] / 255.f; o[2] = src[0] / 255.f; o[3] = src[3] / 255.f; break;  /* D3DCOLOR */
    case 5: for (int k = 0; k < 4; k++) o[k] = src[k]; break;                                                 /* UBYTE4 */
    case 6: { int16_t s[2]; memcpy(s, src, 4); o[0] = s[0]; o[1] = s[1]; break; }                           /* SHORT2 */
    case 7: { int16_t s[4]; memcpy(s, src, 8); for (int k = 0; k < 4; k++) o[k] = s[k]; break; }            /* SHORT4 */
    case 8: for (int k = 0; k < 4; k++) o[k] = src[k] / 255.f; break;                                          /* UBYTE4N */
    case 9: { int16_t s[2]; memcpy(s, src, 4); for (int k = 0; k < 2; k++) o[k] = fmaxf(s[k] / 32767.f, -1.f); break; }   /* SHORT2N */
    case 10: { int16_t s[4]; memcpy(s, src, 8); for (int k = 0; k < 4; k++) o[k] = fmaxf(s[k] / 32767.f, -1.f); break; } /* SHORT4N */
    case 11: { uint16_t s[2]; memcpy(s, src, 4); for (int k = 0; k < 2; k++) o[k] = s[k] / 65535.f; break; }  /* USHORT2N */
    case 12: { uint16_t s[4]; memcpy(s, src, 8); for (int k = 0; k < 4; k++) o[k] = s[k] / 65535.f; break; } /* USHORT4N */
    case 13: { uint32_t v; memcpy(&v, src, 4); o[0] = (float)(v & 0x3FF); o[1] = (float)((v >> 10) & 0x3FF); o[2] = (float)((v >> 20) & 0x3FF); break; } /* UDEC3 */
    case 14: { uint32_t v; memcpy(&v, src, 4);                                                             /* DEC3N */
               for (int k = 0; k < 3; k++) { int x = (int)((v >> (10 * k)) & 0x3FF); if (x & 0x200) x -= 0x400; o[k] = fmaxf(x / 511.f, -1.f); } break; }
    case 15: case 16: { uint16_t h[4]; memcpy(h, src, type == 15 ? 4 : 8);                                   /* FLOAT16_2/4 */
               for (int k = 0; k < (type == 15 ? 2 : 4); k++) {
                   uint32_t sg = (h[k] >> 15) & 1, ex = (h[k] >> 10) & 31, mn = h[k] & 1023;
                   float f = ex == 0 ? ldexpf((float)mn, -24) : ex == 31 ? (mn ? NAN : INFINITY) : ldexpf((float)(mn | 1024), (int)ex - 25);
                   o[k] = sg ? -f : f;
               } break; }
    }
}

/* ------------------------------------------------------------------ execution
 * Four vertices run together: every register component is a 4-lane vector (one lane per vertex), so an instruction is a handful
 * of vector operations and swizzles cost nothing. Relative constant addressing (skinning) gathers per lane. */

typedef float v4f __attribute__((vector_size(16)));
typedef int v4i __attribute__((vector_size(16)));

static inline v4f splat(float x) { return (v4f){ x, x, x, x }; }
static inline float lane(v4f v, int i) { return v[i]; }

typedef struct {
    v4f in[16][4], tmp[12][4], pos[4], d[2][4], tex[8][4], dummy[4];
    int a0[4];
} VsRegs;

static inline void read_src(const VsProgram *p, const Src *s, const VsRegs *R, const float c[96][4], v4f out[4]) {
    switch (s->type) {
    case RT_TEMP: if (s->num < 12) { for (int k = 0; k < 4; k++) out[k] = R->tmp[s->num][s->swz[k]]; break; } goto zero;
    case RT_INPUT: if (s->num < 16) { for (int k = 0; k < 4; k++) out[k] = R->in[s->num][s->swz[k]]; break; } goto zero;
    case RT_CONST:
        if (!s->rel) {
            if (s->num < 96) { for (int k = 0; k < 4; k++) out[k] = splat(c[s->num][s->swz[k]]); break; }
            goto zero;
        } else {
            for (int k = 0; k < 4; k++) {
                float g[4];
                for (int l = 0; l < 4; l++) { int i = s->num + R->a0[l]; g[l] = i >= 0 && i < 96 ? c[i][s->swz[k]] : 0.f; }
                out[k] = (v4f){ g[0], g[1], g[2], g[3] };
            }
            break;
        }
    default: zero:
        for (int k = 0; k < 4; k++) out[k] = splat(0.f);
    }
    if (s->neg) for (int k = 0; k < 4; k++) out[k] = -out[k];
    (void)p;
}

static inline v4f dp3v(const v4f *a, const v4f *b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }
static inline v4f dp4v(const v4f *a, const v4f *b) { return dp3v(a, b) + a[3] * b[3]; }
static inline v4f selv(v4i m, v4f a, v4f b) { return (v4f)(((v4i)a & m) | ((v4i)b & ~m)); }

int vs_trace;   /* set by the renderer to trace one vertex */

/* runs the shader for vertices [from, to) (at most 4 at a time); vertex i is idx[i] (or base + i without idx); out[i - from] */
void vs_run_batch(Shader *sh, const float consts[96][4], const VsStreams *st, const uint32_t *idx, uint32_t base,
                  uint32_t from, uint32_t to, VsOut *out) {
    VsProgram *p = program_for(sh);
    float cbuf[96][4];
    const float (*c)[4] = consts;
    if (p->any_def) {
        memcpy(cbuf, consts, sizeof cbuf);
        for (int i = 0; i < 96; i++) if (p->has_def[i]) memcpy(cbuf[i], p->defs[i], 16);
        c = (const float (*)[4])cbuf;
    }
    for (uint32_t g = from; g < to; g += 4) {
        int n = to - g < 4 ? (int)(to - g) : 4;
        uint32_t vi[4];
        for (int l = 0; l < 4; l++) { uint32_t i = g + (uint32_t)(l < n ? l : n - 1); vi[l] = idx ? idx[i] : base + i; }
        VsRegs R;
        memset(&R, 0, sizeof R);
        for (int i = 0; i < 16; i++) {
            if (!p->in[i].used || !st->data[p->in[i].stream]) { R.in[i][3] = splat(1.f); continue; }
            for (int l = 0; l < 4; l++) {
                float f[4];
                fetch(st->data[p->in[i].stream] + (size_t)vi[l] * st->stride[p->in[i].stream] + p->in[i].offset, p->in[i].type, f);
                for (int k = 0; k < 4; k++) R.in[i][k][l] = f[k];
            }
        }
        if (p->fixed_function) {
            const float *m = st->ff_mvp;
            for (int l = 0; l < n; l++) {
                VsOut *o = &out[g - from + (uint32_t)l];
                float x[4], v5[4], v6[4];
                for (int k = 0; k < 4; k++) { x[k] = R.in[0][k][l]; v5[k] = R.in[5][k][l]; v6[k] = R.in[6][k][l]; }
                for (int k = 0; k < 4; k++) o->pos[k] = x[0] * m[k] + x[1] * m[4 + k] + x[2] * m[8 + k] + m[12 + k];
                if (p->in[5].used) memcpy(o->d0, v5, 16); else for (int k = 0; k < 4; k++) o->d0[k] = 1;
                if (p->in[6].used) memcpy(o->d1, v6, 16); else memset(o->d1, 0, 16);
                for (int t = 0; t < 4; t++) for (int k = 0; k < 4; k++) o->tex[t][k] = R.in[7 + t][k][l];
            }
            continue;
        }
        if (!p->ok) { memset(&out[g - from], 0, sizeof(VsOut) * (size_t)n); continue; }
        R.pos[3] = splat(1.f);
        for (int k = 0; k < 4; k++) { R.d[0][k] = splat(1.f); }
        for (int t = 0; t < 8; t++) R.tex[t][3] = splat(1.f);

        for (uint32_t i = 0; i < p->n; i++) {
            const Insn *in = &p->code[i];
            v4f s0[4], s1[4], s2[4], res[4];
            if (in->nsrc > 0) read_src(p, &in->s[0], &R, c, s0);
            if (in->nsrc > 1) read_src(p, &in->s[1], &R, c, s1);
            if (in->nsrc > 2) read_src(p, &in->s[2], &R, c, s2);
            switch (in->op) {
            case 1: for (int k = 0; k < 4; k++) res[k] = s0[k]; break;                                  /* mov */
            case 2: for (int k = 0; k < 4; k++) res[k] = s0[k] + s1[k]; break;                         /* add */
            case 3: for (int k = 0; k < 4; k++) res[k] = s0[k] - s1[k]; break;                         /* sub */
            case 4: for (int k = 0; k < 4; k++) res[k] = s0[k] * s1[k] + s2[k]; break;                 /* mad */
            case 5: for (int k = 0; k < 4; k++) res[k] = s0[k] * s1[k]; break;                         /* mul */
            case 6: case 7: case 14: case 15: {                                                         /* rcp rsq exp log (scalar, per lane) */
                v4f y;
                for (int l = 0; l < 4; l++) {
                    float x = s0[3][l], r;
                    if (in->op == 6) r = x == 1.f ? 1.f : x == 0.f ? INFINITY : 1.f / x;
                    else if (in->op == 7) { x = fabsf(x); r = x == 1.f ? 1.f : x == 0.f ? INFINITY : 1.f / sqrtf(x); }
                    else if (in->op == 14) r = exp2f(x);
                    else { x = fabsf(x); r = x == 0.f ? -INFINITY : log2f(x); }
                    y[l] = r;
                }
                for (int k = 0; k < 4; k++) res[k] = y;
                break;
            }
            case 8: { v4f d = dp3v(s0, s1); for (int k = 0; k < 4; k++) res[k] = d; break; }
            case 9: { v4f d = dp4v(s0, s1); for (int k = 0; k < 4; k++) res[k] = d; break; }
            case 10: for (int k = 0; k < 4; k++) res[k] = selv(s0[k] < s1[k], s0[k], s1[k]); break;       /* min */
            case 11: for (int k = 0; k < 4; k++) res[k] = selv(s0[k] >= s1[k], s0[k], s1[k]); break;      /* max */
            case 12: for (int k = 0; k < 4; k++) res[k] = selv(s0[k] < s1[k], splat(1.f), splat(0.f)); break;   /* slt */
            case 13: for (int k = 0; k < 4; k++) res[k] = selv(s0[k] >= s1[k], splat(1.f), splat(0.f)); break;  /* sge */
            case 16: {                                                                                  /* lit */
                for (int k = 0; k < 4; k++) res[k] = splat(0.f);
                for (int l = 0; l < 4; l++) {
                    float pw = s0[3][l] < -128.f ? -128.f : s0[3][l] > 128.f ? 128.f : s0[3][l];
                    res[0][l] = 1; res[3][l] = 1;
                    res[1][l] = s0[0][l] > 0 ? s0[0][l] : 0;
                    res[2][l] = s0[0][l] > 0 && s0[1][l] > 0 ? powf(s0[1][l], pw) : 0;
                }
                break;
            }
            case 17: res[0] = splat(1.f); res[1] = s0[1] * s1[1]; res[2] = s0[2]; res[3] = s1[3]; break;   /* dst */
            case 18: for (int k = 0; k < 4; k++) res[k] = s0[k] * (s1[k] - s2[k]) + s2[k]; break;       /* lrp */
            case 19: for (int k = 0; k < 4; k++) for (int l = 0; l < 4; l++) res[k][l] = s0[k][l] - floorf(s0[k][l]); break;   /* frc */
            case 20: case 21: case 22: case 23: case 24: {                                              /* m4x4 m4x3 m3x4 m3x3 m3x2 */
                int rows = in->op == 20 || in->op == 22 ? 4 : in->op == 24 ? 2 : 3, four = in->op <= 21;
                for (int k = 0; k < 4; k++) res[k] = splat(0.f);
                Src row = in->s[1];
                for (int k = 0; k < rows; k++) {
                    v4f m[4];
                    read_src(p, &row, &R, c, m);
                    res[k] = four ? dp4v(s0, m) : dp3v(s0, m);
                    row.num++;
                }
                break;
            }
            case 78: for (int k = 0; k < 4; k++) res[k] = splat(0.f);                                    /* expp */
                     for (int l = 0; l < 4; l++) { float w = s0[3][l], f = floorf(w); res[0][l] = exp2f(f); res[1][l] = w - f; res[2][l] = exp2f(w); res[3][l] = 1; }
                     break;
            case 79: for (int k = 0; k < 4; k++) res[k] = splat(0.f);                                    /* logp */
                     for (int l = 0; l < 4; l++) {
                         float x = fabsf(s0[3][l]);
                         if (x == 0.f) { res[0][l] = res[2][l] = -INFINITY; res[1][l] = 1; }
                         else { int e; float m = frexpf(x, &e); res[0][l] = (float)(e - 1); res[1][l] = m * 2; res[2][l] = log2f(x); }
                         res[3][l] = 1;
                     }
                     break;
            default: for (int k = 0; k < 4; k++) res[k] = splat(0.f); break;
            }
            v4f *dst;
            switch (in->dtype) {
            case RT_TEMP: dst = in->dnum < 12 ? R.tmp[in->dnum] : R.dummy; break;
            case RT_ADDR: if (in->dmask & 1) for (int l = 0; l < 4; l++) R.a0[l] = (int)floorf(res[0][l]); continue;
            case RT_RASTOUT: dst = in->dnum == 0 ? R.pos : R.dummy; break;
            case RT_ATTROUT: dst = in->dnum < 2 ? R.d[in->dnum] : R.dummy; break;
            case RT_TEXOUT: dst = in->dnum < 8 ? R.tex[in->dnum] : R.dummy; break;
            default: dst = R.dummy; break;
            }
            for (int k = 0; k < 4; k++) if (in->dmask & (1 << k)) dst[k] = res[k];
        }
        for (int l = 0; l < n; l++) {
            VsOut *o = &out[g - from + (uint32_t)l];
            for (int k = 0; k < 4; k++) { o->pos[k] = R.pos[k][l]; o->d0[k] = R.d[0][k][l]; o->d1[k] = R.d[1][k][l]; }
            for (int t = 0; t < 4; t++) for (int k = 0; k < 4; k++) o->tex[t][k] = R.tex[t][k][l];
        }
    }
}

void vs_run(Shader *sh, const float consts[96][4], const VsStreams *st, uint32_t index, VsOut *out) {
    vs_run_batch(sh, consts, st, &index, 0, 0, 1, out);
}

/* ------------------------------------------------------------------ on the GPU */

int vs_gpu_inputs(Shader *sh) {
    VsProgram *p = program_for(sh);
    return p->ok && !p->fixed_function ? p->nused : -1;
}

uint32_t vs_stream_mask(Shader *sh) {
    VsProgram *p = program_for(sh);
    uint32_t m = 0;
    for (int u = 0; u < p->nused; u++) m |= 1u << p->in[p->used_in[u]].stream;
    return m;
}

void vs_constants(Shader *sh, const float consts[96][4], float out[96][4]) {
    VsProgram *p = program_for(sh);
    memcpy(out, consts, sizeof(float) * 96 * 4);
    if (p->any_def) for (int i = 0; i < 96; i++) if (p->has_def[i]) memcpy(out[i], p->defs[i], 16);
}

/* the input registers the shader reads, as vec4s in used_in order (the GPU vertex layout), for n vertices:
   vertex i is idx[i], or base + i without idx; out gets 4 * nused floats per vertex. Packed types are
   expanded here, so the GPU side only ever sees float4 attributes. */
void vs_fetch_n(Shader *sh, const VsStreams *st, const uint32_t *idx, uint32_t base, uint32_t n, float *o) {
    VsProgram *p = program_for(sh);
    const uint8_t *data[16];
    uint32_t stride[16], type[16];
    int nu = p->nused;
    for (int u = 0; u < nu; u++) {
        int r = p->used_in[u];
        data[u] = st->data[p->in[r].stream] ? st->data[p->in[r].stream] + p->in[r].offset : NULL;
        stride[u] = st->stride[p->in[r].stream];
        type[u] = p->in[r].type;
    }
    for (uint32_t i = 0; i < n; i++) {
        size_t v = idx ? idx[i] : base + i;
        for (int u = 0; u < nu; u++, o += 4) {
            if (!data[u]) { o[0] = o[1] = o[2] = 0; o[3] = 1; continue; }
            const uint8_t *src = data[u] + v * stride[u];
            switch (type[u]) {   /* the common float types inline, the rest through fetch() */
            case 1: memcpy(o, src, 8); o[2] = 0; o[3] = 1; break;
            case 2: memcpy(o, src, 12); o[3] = 1; break;
            case 3: memcpy(o, src, 16); break;
            case 4: o[0] = src[2] / 255.f; o[1] = src[1] / 255.f; o[2] = src[0] / 255.f; o[3] = src[3] / 255.f; break;
            default: fetch(src, type[u], o); break;
            }
        }
    }
}

typedef struct { char *s; size_t n, cap; } Str;
static void put(Str *b, const char *fmt, ...) {
    va_list ap;
    for (;;) {
        va_start(ap, fmt);
        int k = vsnprintf(b->s ? b->s + b->n : NULL, b->s ? b->cap - b->n : 0, fmt, ap);
        va_end(ap);
        if (k >= 0 && b->s && b->n + (size_t)k < b->cap) { b->n += (size_t)k; return; }
        b->cap = (b->cap + (size_t)(k > 0 ? k : 0)) * 2 + 4096;
        b->s = realloc(b->s, b->cap);
    }
}

/* a source operand as a WGSL vec4 expression (row: register offset for the m4x4-family rows) */
static void src_expr(Str *b, const Src *s, int row) {
    char base[48];
    int num = s->num + row;
    switch (s->type) {
    case RT_TEMP: if (num < 12) snprintf(base, sizeof base, "r%d", num); else strcpy(base, "vec4<f32>(0.0)"); break;
    case RT_INPUT: if (num < 16) snprintf(base, sizeof base, "v%d", num); else strcpy(base, "vec4<f32>(0.0)"); break;
    case RT_CONST:
        if (s->rel) snprintf(base, sizeof base, "cst(%d + a0)", num);
        else if (num < 96) snprintf(base, sizeof base, "vc.c[%d]", num);
        else strcpy(base, "vec4<f32>(0.0)");
        break;
    default: strcpy(base, "vec4<f32>(0.0)"); break;
    }
    static const char xyzw[] = "xyzw";
    char sw[6] = ".";
    if (s->swz[0] == 0 && s->swz[1] == 1 && s->swz[2] == 2 && s->swz[3] == 3) sw[0] = 0;
    else for (int k = 0; k < 4; k++) sw[1 + k] = xyzw[s->swz[k]];
    put(b, s->neg ? "(-(%s%s))" : "(%s%s)", base, sw);
}

/* WGSL for the vertex stage: entry point "vsp", inputs @location(0..nused-1) = the used v registers in
   order, constants in @group(2) (vc.c[96]). Outputs match the fixed-function vertex stage's VO, with the
   same half-pixel offset as the CPU path (render_wgpu.c's vs with flags.x == 2) and colors saturated as
   packing them to D3DCOLOR does. NULL if the shader cannot run on the GPU. */
char *vs_to_wgsl(Shader *sh) {
    VsProgram *p = program_for(sh);
    if (!p->ok || p->fixed_function) return NULL;
    Str b = { 0 };
    put(&b, "struct VC { c: array<vec4<f32>, 96> };\n@group(2) @binding(0) var<uniform> vc: VC;\n"
            "fn cst(i: i32) -> vec4<f32> { if (i < 0 || i >= 96) { return vec4<f32>(0.0); } return vc.c[i]; }\n"
            "fn rcp1(x: f32) -> f32 { if (x == 1.0) { return 1.0; } return 1.0 / x; }\n"
            "fn rsq1(x: f32) -> f32 { let a = abs(x); if (a == 1.0) { return 1.0; } return inverseSqrt(a); }\n"
            "struct VSI {");
    for (int u = 0; u < p->nused; u++) put(&b, "%s @location(%d) v%d: vec4<f32>", u ? "," : "", u, p->used_in[u]);
    if (!p->nused) put(&b, " @builtin(vertex_index) vid: u32");
    put(&b, " };\n@vertex fn vsp(vi: VSI) -> VO {\n");
    for (int i = 0; i < 16; i++) {
        if (p->in[i].used) put(&b, "  let v%d = vi.v%d;\n", i, i);
        else put(&b, "  let v%d = vec4<f32>(0.0, 0.0, 0.0, 1.0);\n", i);
    }
    /* registers as separate variables, not arrays (only constants are indexed dynamically): mobile shader
       compilers can then keep them in registers instead of thread-local memory */
    for (int k = 0; k < 12; k++) put(&b, "  var r%d = vec4<f32>(0.0);\n", k);
    put(&b, "  var a0: i32 = 0;\n  var dummy: vec4<f32>;\n"
            "  var opos = vec4<f32>(0.0, 0.0, 0.0, 1.0);\n  var od0 = vec4<f32>(1.0);\n  var od1 = vec4<f32>(0.0);\n");
    for (int k = 0; k < 8; k++) put(&b, "  var ot%d = vec4<f32>(0.0, 0.0, 0.0, 1.0);\n", k);
    for (uint32_t i = 0; i < p->n; i++) {
        const Insn *in = &p->code[i];
        put(&b, "  {\n");
        for (int k = 0; k < in->nsrc; k++) { put(&b, "    let s%d = ", k); src_expr(&b, &in->s[k], 0); put(&b, ";\n"); }
        put(&b, "    var res = vec4<f32>(0.0);\n");
        switch (in->op) {
        case 1: put(&b, "    res = s0;\n"); break;
        case 2: put(&b, "    res = s0 + s1;\n"); break;
        case 3: put(&b, "    res = s0 - s1;\n"); break;
        case 4: put(&b, "    res = s0 * s1 + s2;\n"); break;
        case 5: put(&b, "    res = s0 * s1;\n"); break;
        case 6: put(&b, "    res = vec4<f32>(rcp1(s0.w));\n"); break;
        case 7: put(&b, "    res = vec4<f32>(rsq1(s0.w));\n"); break;
        case 8: put(&b, "    res = vec4<f32>(dot(s0.xyz, s1.xyz));\n"); break;
        case 9: put(&b, "    res = vec4<f32>(dot(s0, s1));\n"); break;
        case 10: put(&b, "    res = select(s1, s0, s0 < s1);\n"); break;
        case 11: put(&b, "    res = select(s1, s0, s0 >= s1);\n"); break;
        case 12: put(&b, "    res = select(vec4<f32>(0.0), vec4<f32>(1.0), s0 < s1);\n"); break;
        case 13: put(&b, "    res = select(vec4<f32>(0.0), vec4<f32>(1.0), s0 >= s1);\n"); break;
        case 14: put(&b, "    res = vec4<f32>(exp2(s0.w));\n"); break;
        case 15: put(&b, "    res = vec4<f32>(log2(abs(s0.w)));\n"); break;
        case 16: put(&b, "    let pw = clamp(s0.w, -128.0, 128.0);\n"
                         "    res = vec4<f32>(1.0, max(s0.x, 0.0), select(0.0, pow(s0.y, pw), s0.x > 0.0 && s0.y > 0.0), 1.0);\n");
                 break;
        case 17: put(&b, "    res = vec4<f32>(1.0, s0.y * s1.y, s0.z, s1.w);\n"); break;
        case 18: put(&b, "    res = s0 * (s1 - s2) + s2;\n"); break;
        case 19: put(&b, "    res = s0 - floor(s0);\n"); break;
        case 20: case 21: case 22: case 23: case 24: {
            int rows = in->op == 20 || in->op == 22 ? 4 : in->op == 24 ? 2 : 3, four = in->op <= 21;
            for (int k = 0; k < rows; k++) {
                put(&b, "    res.%c = dot(s0%s, ", "xyzw"[k], four ? "" : ".xyz");
                src_expr(&b, &in->s[1], k);
                put(&b, "%s);\n", four ? "" : ".xyz");
            }
            break;
        }
        case 78: put(&b, "    let w = s0.w; let fw = floor(w);\n    res = vec4<f32>(exp2(fw), w - fw, exp2(w), 1.0);\n"); break;
        case 79: put(&b, "    let x = abs(s0.w); let e = floor(log2(x));\n"
                         "    res = vec4<f32>(e, x / exp2(e), log2(x), 1.0);\n"); break;
        default: break;
        }
        if (in->dtype == RT_ADDR) {
            if (in->dmask & 1) put(&b, "    a0 = i32(floor(res.x));\n");
        } else {
            char dst[24];
            switch (in->dtype) {
            case RT_TEMP: if (in->dnum < 12) snprintf(dst, sizeof dst, "r%u", (unsigned)in->dnum); else strcpy(dst, "dummy"); break;
            case RT_RASTOUT: strcpy(dst, in->dnum == 0 ? "opos" : "dummy"); break;
            case RT_ATTROUT: if (in->dnum < 2) snprintf(dst, sizeof dst, "od%u", (unsigned)in->dnum); else strcpy(dst, "dummy"); break;
            case RT_TEXOUT: if (in->dnum < 8) snprintf(dst, sizeof dst, "ot%u", (unsigned)in->dnum); else strcpy(dst, "dummy"); break;
            default: strcpy(dst, "dummy"); break;
            }
            if (in->dmask == 0xF) put(&b, "    %s = res;\n", dst);
            else for (int k = 0; k < 4; k++) if (in->dmask & (1 << k)) put(&b, "    %s.%c = res.%c;\n", dst, "xyzw"[k], "xyzw"[k]);
        }
        put(&b, "  }\n");
    }
    put(&b, "  var o: VO;\n  var q = opos;\n  q.x = q.x + q.w / u.vp.z;\n  q.y = q.y - q.w / u.vp.w;\n  o.pos = q;\n"
            "  o.diffuse = saturate(od0);\n  o.specular = saturate(od1);\n"
            "  o.tc0 = ot0; o.tc1 = ot1; o.tc2 = ot2; o.tc3 = ot3;\n  return o;\n}\n");
    return b.s;
}

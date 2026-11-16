/* Direct3D 8 vertex shaders, on the CPU or translated for the GPU (see vshader.c). */
#ifndef RC_VSHADER_H
#define RC_VSHADER_H

#include "d3d9.h"

typedef struct {
    const uint8_t *data[16];   /* per stream: host pointer to vertex 0 */
    uint32_t stride[16];
    const float *ff_mvp;       /* world*view*projection, for declaration-only (fixed-function) shaders */
} VsStreams;

typedef struct { float pos[4], d0[4], d1[4], tex[4][4]; } VsOut;

void vs_run(Shader *sh, const float consts[96][4], const VsStreams *st, uint32_t index, VsOut *out);
void vs_run_batch(Shader *sh, const float consts[96][4], const VsStreams *st, const uint32_t *idx, uint32_t base,
                  uint32_t from, uint32_t to, VsOut *out);   /* vertices [from, to): idx[i], or base + i without idx; out[i - from] */
int vs_is_fixed_function(Shader *sh);

/* GPU execution: the number of input registers the shader reads (-1: it must run on the CPU), the vertex
   streams those come from (bit per stream), its constants for one draw (the device's, with the shader's
   own def/declaration values over them), fetching its inputs for n vertices (idx[i], or base + i without
   idx; 4 floats per input per vertex) and its WGSL vertex function (malloc'd; NULL if not translatable) */
int vs_gpu_inputs(Shader *sh);
uint32_t vs_stream_mask(Shader *sh);
void vs_constants(Shader *sh, const float consts[96][4], float out[96][4]);
void vs_fetch_n(Shader *sh, const VsStreams *st, const uint32_t *idx, uint32_t base, uint32_t n, float *out);
char *vs_to_wgsl(Shader *sh);

#endif

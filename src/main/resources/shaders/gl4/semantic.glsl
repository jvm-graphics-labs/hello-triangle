
// Attributes
#define POSITION            0
#define COLOR               1
#define NORMAL              2
#define TEXCOORD            3
#define DRAW_ID             4

// Uniform
#define TRANSFORM0  1
#define TRANSFORM1  2

// Samplers
#define DIFFUSE 0

// Interfaces
#define BLOCK       0

// Outputs
#define FRAG_COLOR 0


precision highp float;
precision highp int;

layout(std140, column_major) uniform;
layout(std430, column_major) buffer;



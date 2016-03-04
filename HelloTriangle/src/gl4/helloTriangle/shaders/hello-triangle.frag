/*
 * Fragment shader.
 */
#version 450

// Interfaces
#define BLOCK       0
// Output
#define FRAG_COLOR  0

precision highp float;
precision highp int;
layout(std140, column_major) uniform;
layout(std430, column_major) buffer;

// Incoming interpolated (between vertices) color.
layout (location = BLOCK) in Block
{
    vec3 interpolatedColor;
} block;

// Outgoing final color.
layout (location = FRAG_COLOR) out vec4 outputColor;

void main() {
    // We simply pad the interpolatedColor
    outputColor = vec4(block.interpolatedColor, 1);
}

/*
 * Vertex shader.
 */
#version 450
// Vertex attributes
#define POSITION    0
#define COLOR       3
// Uniform
#define TRANSFORM0  1
// Interfaces
#define BLOCK       0

precision highp float;
precision highp int;
layout(std140, column_major) uniform;
layout(std430, column_major) buffer;

layout (location = POSITION) in vec2 position; // Incoming vertex position, Model Space.
layout (location = COLOR) in vec3 color; // Incoming vertex color.

// Uniform matrix from Model Space to Clip Space.
layout (binding = TRANSFORM0) uniform Transform
{
    mat4 modelToClipMatrix;
} transform;

// Outgoing color.
layout (location = BLOCK) out Block
{
    vec3 interpolatedColor;
} block;

void main() {

    // Normally gl_Position is in Clip Space and we calculate it by multiplying 
    // it with the modelToClipMatrix.
    gl_Position = transform.modelToClipMatrix * vec4(position, 0, 1);

    // We assign the color to the outgoing variable.
    block.interpolatedColor = color;
}

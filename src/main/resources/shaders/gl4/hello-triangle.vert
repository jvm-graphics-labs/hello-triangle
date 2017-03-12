
#version 450

#include semantic.glsl

// Incoming vertex position, Model Space.
layout (location = POSITION) in vec2 position;

// Incoming vertex color.
layout (location = COLOR) in vec3 color;

// Projection and view matrices.
layout (binding = TRANSFORM0) uniform Transform0
{
    mat4 proj;
    mat4 view;
};

// model matrix
layout (binding = TRANSFORM1) uniform Transform1
{
    mat4 model;
};

// Outgoing color.
layout (location = BLOCK) out Block
{
    vec3 interpolatedColor;
};

void main() {

    // Normally gl_Position is in Clip Space and we calculate it by multiplying together all the matrices
//    gl_Position = proj * (view * (model * vec4(position, 0, 1)));
    gl_Position = model * vec4(position, 0, 1);

    // We assign the color to the outgoing variable.
    interpolatedColor = color;
}

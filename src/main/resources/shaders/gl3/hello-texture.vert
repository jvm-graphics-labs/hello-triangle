
#version 330


#include semantic.glsl


// Incoming vertex position, Model Space
layout (location = POSITION) in vec2 position;

// Incoming vertex color
layout (location = TEXCOORD) in vec2 texCoord;


uniform GlobalMatrices
{
    mat4 proj;
    mat4 view;
};


// Uniform matrix from Model Space to camera (also known as view) Space
uniform mat4 model;


// Outgoing texture coordinates.
out vec2 interpolatedTexCoord;


void main() {

    // Normally gl_Position is in Clip Space and we calculate it by multiplying together all the matrices
    gl_Position = proj * (view * (model * vec4(position, 0, 1)));

    // We assign the texture coordinate to the outgoing variable.
    interpolatedTexCoord = texCoord;
}
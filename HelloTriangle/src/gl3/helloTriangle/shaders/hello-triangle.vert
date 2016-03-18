/*
 * Vertex shader.
 */
#version 330

// Incoming vertex position, Model Space.
in vec2 position;
// Incoming vertex color.
in vec3 color;

// Uniform matrix from Model Space to Clip Space.
uniform mat4 modelToClipMatrix;

// Outgoing color.
out vec3 interpolatedColor;

void main() {

    // Normally gl_Position is in Clip Space and we calculate it by multiplying 
    // it with the modelToClipMatrix.
    gl_Position = modelToClipMatrix * vec4(position, 0, 1);

    // We assign the color to the outgoing variable.
    interpolatedColor = color;
}

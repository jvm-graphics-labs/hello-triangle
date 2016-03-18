/*
 * Vertex shader.
 */
#version 330

// Incoming vertex position, Model Space.
in vec2 position;
// Incoming texture coordinates.
in vec2 texCoord;

// Uniform matrix from Model Space to Clip Space.
uniform mat4 modelToClipMatrix;

// Outgoing texture coordinates.
out vec2 interpolatedTexCoord;

void main() {

    // Normally gl_Position is in Clip Space and we calculate it by multiplying 
    // it with the modelToClipMatrix.
    gl_Position = modelToClipMatrix * vec4(position, 0, 1);

    // We assign the texture coordinate to the outgoing variable.
    interpolatedTexCoord = texCoord;
}

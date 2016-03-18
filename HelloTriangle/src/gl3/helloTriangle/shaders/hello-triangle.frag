/*
 * Fragment shader.
 */

#version 330

// Incoming interpolated (between vertices) color.
in vec3 interpolatedColor;

// Outgoing final color.
out vec4 outputColor;

void main() {
    // We simply pad the interpolatedColor
    outputColor = vec4(interpolatedColor, 1);
}

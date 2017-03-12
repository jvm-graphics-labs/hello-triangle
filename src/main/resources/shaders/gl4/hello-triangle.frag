
#version 450


#include semantic.glsl


// Incoming interpolated (between vertices) color.
layout (location = BLOCK) in Block
{
    vec3 interpolatedColor;
};

// Outgoing final color.
layout (location = FRAG_COLOR) out vec4 outputColor;


void main()
{
    // We simply pad the interpolatedColor
    outputColor = vec4(interpolatedColor, 1);
}

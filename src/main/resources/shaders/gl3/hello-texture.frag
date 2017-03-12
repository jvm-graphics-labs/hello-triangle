
#version 330


#include semantic.glsl


// Incoming interpolated (between vertices) texture coordinates.
in vec2 interpolatedTexCoord;


// Uniform 2D sampler for our texture object.
uniform sampler2D diffuse;


// Outgoing final color.
layout (location = FRAG_COLOR) out vec4 outputColor;


void main()
{
    // We sample texture0 at the interpolatedTexCoord
    outputColor = texture(diffuse, interpolatedTexCoord);
}

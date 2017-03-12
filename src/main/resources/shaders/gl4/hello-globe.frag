
#version 450 core

#include semantic.glsl


layout (binding = DIFFUSE) uniform sampler2D globe;


in Block
{
    vec2 texCoord_;
};


layout(location = FRAG_COLOR, index = 0) out vec4 color;


void main()
{
    color = texture(globe, texCoord_);
}

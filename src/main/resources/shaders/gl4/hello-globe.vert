
#version 450 core

#include semantic.glsl


// Incoming vertex position, Model Space.
layout (location = POSITION) in vec3 position;

// Incoming texture coordinate.
layout (location = TEXCOORD) in vec2 texCoord;


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


out gl_PerVertex
{
    vec4 gl_Position;
};

out Block
{
    vec2 texCoord_;
};

void main()
{	
    //gl_Position = vec4(0.5f * (gl_VertexID % 2) - 0.5f, 0.5f * (gl_VertexID / 2) - 0.5f, 0.0, 1.0);
    gl_Position = proj * (view * (model * vec4(position, 1)));
    
    texCoord_ = texCoord;
}

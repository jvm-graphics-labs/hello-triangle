#version 450 core

#define POSITION	0
#define TEXCOORD	4
#define TRANSFORM0	1

precision highp float;
precision highp int;
layout(std140, column_major) uniform;

layout(binding = TRANSFORM0) uniform Transform
{
    mat4 mvp;
} transform;

layout(location = POSITION) in vec3 position;
layout(location = TEXCOORD) in vec2 texCoord;

out gl_PerVertex
{
    vec4 gl_Position;
};

out Block
{
    vec2 texCoord;
} outBlock;

void main()
{	
    //gl_Position = vec4(0.5f * (gl_VertexID % 2) - 0.5f, 0.5f * (gl_VertexID / 2) - 0.5f, 0.0, 1.0);
    gl_Position = transform.mvp * vec4(position, 1.0);
    
    outBlock.texCoord = texCoord;
}

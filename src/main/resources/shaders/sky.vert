#version 330 core

layout (location = 0) in vec2 aPos;

uniform mat4 u_InvViewProj;

out vec3 v_RayDir;

void main() {
    gl_Position = vec4(aPos, 1.0, 1.0);
    vec4 world = u_InvViewProj * vec4(aPos, 1.0, 1.0);
    v_RayDir = world.xyz / world.w;
}


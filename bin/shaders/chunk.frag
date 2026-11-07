#version 330 core

in vec3 v_TexCoord;
in float v_AO;
in float v_Light;
in float v_Distance;

uniform sampler2DArray u_TextureArray;
uniform vec3 u_FogColor;
uniform float u_FogStart;
uniform float u_FogEnd;

out vec4 FragColor;

void main() {
    vec4 texColor = texture(u_TextureArray, v_TexCoord);

    // Alpha cutout for foliage/leaves and glass
    if (texColor.a < 0.1) {
        discard;
    }

    // Apply lighting and smooth ambient occlusion
    vec3 color = texColor.rgb * v_Light * v_AO;

    // Atmospheric distance fog
    float fogFactor = clamp((v_Distance - u_FogStart) / (u_FogEnd - u_FogStart), 0.0, 1.0);
    // Smoothstep curve for soft horizon transition
    fogFactor = smoothstep(0.0, 1.0, fogFactor);
    color = mix(color, u_FogColor, fogFactor);

    FragColor = vec4(color, texColor.a);
}


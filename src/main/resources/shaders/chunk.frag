#version 330 core

in vec3 v_WorldPos;
in vec3 v_Normal;
in vec3 v_TexCoord;
in float v_AO;
in float v_Distance;
in float v_Light;

uniform sampler2DArray u_TextureArray;
uniform vec3 u_CameraPos;
uniform vec3 u_PlayerPos;        // Player feet position for dynamic contact shadow
uniform vec3 u_LightDir;         // Direction to sun/moon
uniform vec3 u_LightColor;       // Color of direct celestial light
uniform vec3 u_AmbientColor;     // Base ambient light color
uniform vec3 u_FogColor;         // Fog & sky color
uniform float u_FogStart;
uniform float u_FogEnd;

// Dynamic Handheld Torch Uniforms
uniform vec3 u_HeldTorchPos;
uniform vec3 u_HeldTorchLight;   // Warm flame color & intensity, 0 if not holding torch

// Dynamic Placed Torch Uniforms (up to 16 nearest torches)
uniform int u_NumPlacedTorches;
uniform vec3 u_PlacedTorches[16];
uniform float u_Time;

out vec4 FragColor;

void main() {
    vec4 texColor = texture(u_TextureArray, v_TexCoord);

    // Alpha cutout for foliage, leaves, glass, and torch stick/flame
    if (texColor.a < 0.1) {
        discard;
    }

    vec3 N = normalize(v_Normal);
    vec3 L = normalize(u_LightDir);
    vec3 V = normalize(u_CameraPos - v_WorldPos);
    vec3 H = normalize(L + V);
    vec3 R = reflect(-V, N);

    int texLayer = int(v_TexCoord.z + 0.5);

    // 1. Authentic Minecraft Smooth Lighting Curve
    // Smooth power curve converts 0..1 light into realistic perceptually uniform illumination
    float lightFactor = mix(0.06, 1.0, pow(v_Light, 1.55));

    // Direct celestial light (sun/moon) reaches blocks based on normal and vertex sky light
    float NdotL = max(dot(N, L), 0.0);
    vec3 directLight = u_LightColor * NdotL * pow(v_Light, 1.8) * (v_AO * 0.65 + 0.35);

    // Ambient light: smoothly diffused across corners and surfaces
    vec3 ambientLight = u_AmbientColor * lightFactor * v_AO;
    vec3 totalLight = directLight + ambientLight;

    // Torches are self-illuminating light emitters!
    if (texLayer == 20) {
        totalLight = vec3(1.4); // Bright self-glowing flame
    }

    vec3 albedo = texColor.rgb;
    float alpha = texColor.a;

    // Environmental sky reflection vector color
    float skyGradient = clamp(R.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 envSky = mix(u_FogColor, u_LightColor * 0.85, pow(max(dot(R, L), 0.0), 4.0) * 0.40 + skyGradient * 0.20);

    // 2. Reflective Shader Effects across ALL surfaces
    if (texLayer == 9) {
        // === WATER SHADING ===
        albedo = vec3(0.10, 0.42, 0.75);

        // Specular sun glint (scaled by vertex light)
        float NdotH = max(dot(N, H), 0.0);
        float spec = pow(NdotH, 96.0) * NdotL * v_Light;
        vec3 sunSpecular = u_LightColor * spec * 2.8;

        // Fresnel reflection (Schlick's approximation)
        float NdotV = max(dot(N, V), 0.0);
        float F0 = 0.03;
        float fresnel = F0 + (1.0 - F0) * pow(clamp(1.0 - NdotV, 0.0, 1.0), 5.0);

        // Combine water body with environment sky reflection and sun glint
        albedo = mix(albedo * totalLight, envSky, fresnel * 0.72) + sunSpecular;
        alpha = clamp(0.48 + fresnel * 0.44, 0.0, 0.95);

    } else if (texLayer != 20) {
        // === ALL GROUND & SOLID BLOCKS REFLECTIVE ===
        albedo = albedo * totalLight;

        // Blinn-Phong specular sheen from sun/moon
        float NdotH = max(dot(N, H), 0.0);
        float shininess = (texLayer == 3 || texLayer == 4 || texLayer == 10) ? 28.0 : 16.0;
        float specStrength = (texLayer == 3 || texLayer == 4 || texLayer == 10 || texLayer == 11) ? 0.22 : 0.10;
        float groundSpec = pow(NdotH, shininess) * NdotL * v_Light;
        vec3 specular = u_LightColor * groundSpec * specStrength * v_AO;

        // Environmental ambient reflection across ground
        float NdotV = max(dot(N, V), 0.0);
        float groundFresnel = 0.03 + 0.12 * pow(clamp(1.0 - NdotV, 0.0, 1.0), 4.0);
        vec3 ambientReflection = envSky * groundFresnel * 0.16 * v_AO * lightFactor;

        albedo += specular + ambientReflection;
    }

    // 3. Dynamic Handheld Torchlight (when holding torch in slot 3)
    if (length(u_HeldTorchLight) > 0.01) {
        float d = distance(v_WorldPos, u_HeldTorchPos);
        if (d < 16.0) {
            float atten = clamp(1.0 - d / 16.0, 0.0, 1.0);
            float intensity = (atten * atten * 0.75 + atten * 0.25);
            vec3 torchDir = normalize(u_HeldTorchPos - v_WorldPos);
            float NdotT = max(dot(N, torchDir), 0.28);
            albedo += u_HeldTorchLight * intensity * NdotT * 1.5;
        }
    }

    // 4. Dynamic Placed Torchlights (placed torches in the world)
    for (int i = 0; i < u_NumPlacedTorches; i++) {
        float d = distance(v_WorldPos, u_PlacedTorches[i]);
        if (d < 14.0) {
            float atten = clamp(1.0 - d / 14.0, 0.0, 1.0);
            float intensity = (atten * atten);
            vec3 torchDir = normalize(u_PlacedTorches[i] - v_WorldPos);
            float NdotT = max(dot(N, torchDir), 0.28);
            float flicker = 1.0 + 0.04 * sin(u_Time * 14.0 + float(i) * 2.1);
            vec3 torchColor = vec3(1.0, 0.65, 0.24) * flicker;
            albedo += torchColor * intensity * NdotT * 1.6;
        }
    }

    // 5. Player Dynamic Contact Shadow on Ground
    float dy = u_PlayerPos.y - v_WorldPos.y;
    if (v_Normal.y > 0.65 && dy >= -0.2 && dy < 4.0) {
        vec2 shadowCenter = u_PlayerPos.xz + u_LightDir.xz * min(max(dy, 0.0), 2.5) * 0.20;
        float pDist = length(v_WorldPos.xz - shadowCenter);
        float radius = mix(0.55, 0.85, clamp(dy / 4.0, 0.0, 1.0));
        if (pDist < radius) {
            float falloff = 1.0 - smoothstep(radius * 0.2, radius, pDist);
            float intensity = mix(0.60, 0.20, clamp(dy / 4.0, 0.0, 1.0));
            albedo *= (1.0 - falloff * intensity);
        }
    }

    // 6. Smooth Atmospheric Distance Fog
    float fogFactor = clamp((v_Distance - u_FogStart) / (u_FogEnd - u_FogStart), 0.0, 1.0);
    fogFactor = smoothstep(0.0, 1.0, fogFactor);
    vec3 finalColor = mix(albedo, u_FogColor, fogFactor);

    FragColor = vec4(finalColor, alpha);
}

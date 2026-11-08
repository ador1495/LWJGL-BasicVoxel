#version 330 core

in vec3 v_RayDir;

uniform vec3 u_SunDir;        // Direction towards sun
uniform vec3 u_LightColor;    // Direct sunlight/moonlight color
uniform vec3 u_SkyColor;      // Horizon/fog color from DaylightCycle
uniform vec3 u_ZenithColor;   // Deep zenith sky color
uniform float u_Time;         // Running time for star twinkle

out vec4 FragColor;

// High-speed pseudo-random directional hash for stars
float hash31(vec3 p) {
    p = fract(p * vec3(443.897, 441.423, 437.195));
    p += dot(p, p.zxy + 19.19);
    return fract((p.x + p.y) * p.z);
}

void main() {
    vec3 dir = normalize(v_RayDir);

    // 1. Atmospheric Horizon to Zenith Gradient
    float elevation = clamp(dir.y, 0.0, 1.0);
    float skyCurve = pow(elevation, 0.60);
    vec3 sky = mix(u_SkyColor, u_ZenithColor, skyCurve);

    // Below horizon fade (smooth transition towards ground)
    if (dir.y < 0.0) {
        float groundFade = clamp(1.0 + dir.y * 4.0, 0.0, 1.0);
        sky = mix(u_SkyColor * 0.40, u_SkyColor, groundFade);
    }

    // 2. Sunset / Sunrise Golden Horizon Glow
    float sunElev = u_SunDir.y;
    float sunsetFactor = clamp(1.0 - abs(sunElev) * 4.5, 0.0, 1.0);
    if (sunsetFactor > 0.0) {
        float sunHorizDot = max(dot(dir, normalize(vec3(u_SunDir.x, 0.05, u_SunDir.z))), 0.0);
        vec3 sunsetGlow = vec3(1.0, 0.42, 0.16) * pow(sunHorizDot, 10.0) * (1.0 - elevation * 0.7) * sunsetFactor;
        sky += sunsetGlow;
    }

    // 3. The Sun (Radiant celestial body with coronal halo)
    float sunDot = dot(dir, u_SunDir);
    if (sunDot > 0.88 && u_SunDir.y > -0.20) {
        // Sharp circular sun disk
        float sunDisk = smoothstep(0.9980, 0.9992, sunDot);
        // Brilliant warm corona
        float sunCorona = pow(max(sunDot, 0.0), 40.0) * 0.55 + pow(max(sunDot, 0.0), 8.0) * 0.22;
        vec3 sunColor = vec3(1.0, 0.96, 0.84);
        vec3 sunEffect = sunColor * sunDisk * 3.2 + u_LightColor * sunCorona * 1.5;
        // Fade smoothly at horizon
        float hFade = smoothstep(-0.04, 0.06, dir.y);
        sky += sunEffect * hFade;
    }

    // 4. The Moon (Silver celestial body with lunar craters and cool glow)
    vec3 moonDir = -u_SunDir;
    float moonDot = dot(dir, moonDir);
    if (moonDot > 0.90 && moonDir.y > -0.20) {
        // Antialiased moon disk
        float moonDisk = smoothstep(0.9982, 0.9993, moonDot);

        // Procedural craters on lunar surface
        vec3 right = normalize(cross(vec3(0.0, 1.0, 0.0), moonDir));
        vec3 up = cross(moonDir, right);
        vec2 mUV = vec2(dot(dir, right), dot(dir, up)) * 42.0;
        float craters = sin(mUV.x * 2.4) * cos(mUV.y * 2.4) * 0.16 +
                        sin(mUV.x * 5.5 + 1.0) * sin(mUV.y * 4.8) * 0.10;
        vec3 moonSurface = vec3(0.88, 0.92, 1.0) * clamp(1.0 - craters, 0.68, 1.0);

        // Silver-blue lunar halo
        float moonHalo = pow(max(moonDot, 0.0), 36.0) * 0.40 + pow(max(moonDot, 0.0), 10.0) * 0.15;
        vec3 moonEffect = moonSurface * moonDisk * 2.4 + vec3(0.60, 0.75, 1.0) * moonHalo;
        float hFade = smoothstep(-0.04, 0.06, dir.y);
        sky += moonEffect * hFade;
    }

    // 5. Star Field at Night
    float nightFactor = clamp(-u_SunDir.y * 3.2, 0.0, 1.0);
    if (nightFactor > 0.0 && dir.y > 0.02) {
        vec3 starCoord = dir * 180.0;
        vec3 grid = floor(starCoord);
        float h = hash31(grid);
        if (h > 0.982) {
            float twinkle = sin(u_Time * 2.8 + h * 6.28) * 0.30 + 0.70;
            float starBrightness = pow((h - 0.982) / 0.018, 2.5) * nightFactor * twinkle * smoothstep(0.02, 0.16, dir.y);
            vec3 starTint = mix(vec3(1.0), vec3(0.82, 0.90, 1.0), fract(h * 37.0));
            sky += starTint * starBrightness * 1.8;
        }
    }

    FragColor = vec4(sky, 1.0);
}


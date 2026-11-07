#version 330 core

layout (location = 0) in int aPacked;

uniform mat4 u_ViewProjection;
uniform vec3 u_ChunkOffset;
uniform vec3 u_CameraPos;

out vec3 v_TexCoord;
out float v_AO;
out float v_Light;
out float v_Distance;

const float FACE_LIGHT[6] = float[6](
    1.0,   // 0: UP (+Y)
    0.50,  // 1: DOWN (-Y)
    0.80,  // 2: NORTH (-Z)
    0.80,  // 3: SOUTH (+Z)
    0.68,  // 4: WEST (-X)
    0.68   // 5: EAST (+X)
);

const float AO_FACTORS[4] = float[4](
    0.45, 0.65, 0.82, 1.0
);

const vec2 UV_CORNERS[4] = vec2[4](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(1.0, 1.0),
    vec2(0.0, 1.0)
);

void main() {
    float x = float(aPacked & 0x1F);
    float y = float((aPacked >> 5) & 0x1F);
    float z = float((aPacked >> 10) & 0x1F);
    int face = (aPacked >> 15) & 0x07;
    int texLayer = (aPacked >> 18) & 0x7F;
    int ao = (aPacked >> 25) & 0x03;
    int uvIndex = (aPacked >> 27) & 0x03;

    vec3 worldPos = u_ChunkOffset + vec3(x, y, z);
    gl_Position = u_ViewProjection * vec4(worldPos, 1.0);

    vec2 uv = UV_CORNERS[uvIndex];
    v_TexCoord = vec3(uv.x, uv.y, float(texLayer));
    v_AO = AO_FACTORS[ao];
    v_Light = FACE_LIGHT[face];
    v_Distance = distance(worldPos, u_CameraPos);
}


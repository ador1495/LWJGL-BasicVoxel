#version 330 core

layout (location = 0) in int aPacked;

uniform mat4 u_ViewProjection;
uniform vec3 u_ChunkOffset;
uniform vec3 u_CameraPos;

out vec3 v_WorldPos;
out vec3 v_Normal;
out vec3 v_TexCoord;
out float v_AO;
out float v_Distance;
out float v_Light;

const vec3 NORMALS[6] = vec3[6](
    vec3( 0.0,  1.0,  0.0), // 0: UP (+Y)
    vec3( 0.0, -1.0,  0.0), // 1: DOWN (-Y)
    vec3( 0.0,  0.0, -1.0), // 2: NORTH (-Z)
    vec3( 0.0,  0.0,  1.0), // 3: SOUTH (+Z)
    vec3(-1.0,  0.0,  0.0), // 4: WEST (-X)
    vec3( 1.0,  0.0,  0.0)  // 5: EAST (+X)
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
    int texLayer = (aPacked >> 18) & 0x1F;
    int ao = (aPacked >> 23) & 0x03;
    int uvIndex = (aPacked >> 25) & 0x03;
    int light = (aPacked >> 27) & 0x0F;

    vec3 worldPos = u_ChunkOffset + vec3(x, y, z);
    gl_Position = u_ViewProjection * vec4(worldPos, 1.0);

    vec2 uv = UV_CORNERS[uvIndex];
    v_WorldPos = worldPos;
    v_Normal = NORMALS[face];
    v_TexCoord = vec3(uv.x, uv.y, float(texLayer));
    v_AO = AO_FACTORS[ao];
    v_Light = float(light) / 15.0;
    v_Distance = distance(worldPos, u_CameraPos);
}

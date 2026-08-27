#version 460

layout(binding = 0, std140) uniform SceneUniform {
    mat4 MVP;
    ivec4 section;
    vec4 negInnerSec;
    int boundaryBuffer;  // Configurable safety margin (0-4 blocks)
};

layout(binding = 1, std430) restrict readonly buffer ChunkPosBuffer {
    ivec2[] chunkPos;
};

ivec3 unpackPos(ivec2 pos) {
    return ivec3(pos.y>>10, (pos.x<<12)>>12, ((pos.y<<22)|int(uint(pos.x)>>10))>>10);
}

bool shouldRender(ivec3 icorner) {
    int buf = max(boundaryBuffer, 1);
    ivec3 minCorner = icorner - buf;
    ivec3 maxCorner = icorner + 16 + buf;

    vec3 corner = vec3(mix(mix(ivec3(0), minCorner, greaterThan(minCorner, ivec3(0))), maxCorner, lessThan(maxCorner, ivec3(0)))) - negInnerSec.xyz;
    bool visible = (corner.x*corner.x + corner.z*corner.z) < (negInnerSec.w*negInnerSec.w);
    visible = visible && abs(corner.y) < negInnerSec.w;
    return visible;
}

#ifdef TAA
vec2 getTAA();
#endif

void main() {
    uint id = (gl_InstanceID<<5)+gl_BaseInstance+(gl_VertexID>>3);

    ivec3 origin = unpackPos(chunkPos[id])*16;
    origin -= section.xyz;

    if (!shouldRender(origin)) {
        gl_Position = vec4(-100.0f, -100.0f, -100.0f, 0.0f);
        return;
    }

    // Inset bounding box by 1.0 block horizontally to allow LOD quads to overlap and seal chunk boundaries.
    // This completely eliminates see-through gaps on water surfaces, cliff edges, and sand slopes.
    // The GPU hardware depth test (GL_LEQUAL) naturally hides any overlapping LOD geometry behind vanilla terrain.
    float xCorner = ((gl_VertexID & 1) != 0) ? 15.0 : 1.0;
    float yCorner = (((gl_VertexID >> 2) & 1) != 0) ? 16.0 : 0.0;
    float zCorner = (((gl_VertexID >> 1) & 1) != 0) ? 15.0 : 1.0;
    vec3 cubeCorner = vec3(xCorner, yCorner, zCorner);

    gl_Position = MVP * vec4(cubeCorner + vec3(origin), 1.0);

    #ifdef TAA
    gl_Position.xy += getTAA()*gl_Position.w;//Apply TAA if we have it
    #endif
}
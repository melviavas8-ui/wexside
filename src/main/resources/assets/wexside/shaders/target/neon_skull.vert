#version 330 core
layout(std140) uniform SkullData { mat4 mvp; vec4 tint; vec4 animation; vec4 ending; };
out vec2 point;
void main() {
    vec2 corners[6] = vec2[](vec2(-1,-1),vec2(1,-1),vec2(1,1),vec2(-1,-1),vec2(1,1),vec2(-1,1));
    point = corners[gl_VertexID] * vec2(1.25, 1.4);
    gl_Position = mvp * vec4(point, 0.0, 1.0);
}

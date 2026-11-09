#version 330
in vec3 Position;
in vec2 UV0;
out vec2 uv;
layout(std140) uniform CylinderData { mat4 mvp; vec4 meta; vec4 tint; vec4 shape; };
void main() {
    uv = UV0;
    vec3 position = vec3(Position.x * shape.x, UV0.y < 0.5 ? shape.y : shape.z, Position.z * shape.x);
    gl_Position = mvp * vec4(position, 1.0);
}

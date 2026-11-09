#version 330
in vec3 Position;
in vec4 Color;
in vec2 UV0;
out vec4 vertexColor;
noperspective out float edgePx;
noperspective out float halfPx;
layout(std140) uniform ArcData { vec4 tint; };
void main() {
    vertexColor = Color * tint;
    edgePx = UV0.x;
    halfPx = UV0.y;
    gl_Position = vec4(Position, 1.0);
}

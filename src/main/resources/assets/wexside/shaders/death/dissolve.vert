#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection { mat4 ProjMat; };

in vec3 Position;
in vec4 Color;
in vec2 UV0;

out vec4 tint;
out vec2 animation;

void main() {
    vec4 position = ModelViewMat * vec4(Position, 1.0);
    tint = Color * ColorModulator;
    animation = UV0;
    gl_Position = ProjMat * position;
}

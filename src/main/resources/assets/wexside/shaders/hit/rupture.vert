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
in ivec2 UV1;
in ivec2 UV2;
out vec4 tint;
out vec3 surfacePos;
out vec2 animation;
out vec3 viewPos;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    viewPos = (ModelViewMat * vec4(Position, 1.0)).xyz;
    tint = Color * ColorModulator;
    surfacePos = vec3(vec2(UV1), float(UV2.x)) / 2048.0;
    animation = UV0;
}

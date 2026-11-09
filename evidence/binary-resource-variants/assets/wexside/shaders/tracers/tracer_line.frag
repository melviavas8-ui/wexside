#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec4 vertexColor;
noperspective in float edgeDistance;
flat in float lineHalfWidth;

out vec4 fragColor;

void main() {
    float distanceFromCenter = abs(edgeDistance);
    float antialiasWidth = max(fwidth(distanceFromCenter) * 0.75, 0.5);
    float coverage = 1.0 - smoothstep(
        max(lineHalfWidth - antialiasWidth, 0.0),
        lineHalfWidth + antialiasWidth,
        distanceFromCenter
    );

    fragColor = vertexColor * ColorModulator;
    fragColor.a *= coverage;
}

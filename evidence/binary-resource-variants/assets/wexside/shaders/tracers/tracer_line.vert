#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

layout(std140) uniform Projection {
    mat4 ProjMat;
};

layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    vec3 CameraOffset;
    vec2 ScreenSize;
    float GlintAlpha;
    float GameTime;
    int MenuBlurRadius;
    int UseRgss;
};

in vec3 Position;
in vec4 Color;
in vec3 Normal;
in float LineWidth;

out vec4 vertexColor;
noperspective out float edgeDistance;
flat out float lineHalfWidth;

const float VIEW_SHRINK = 1.0 - (1.0 / 256.0);
const mat4 VIEW_SCALE = mat4(
    VIEW_SHRINK, 0.0, 0.0, 0.0,
    0.0, VIEW_SHRINK, 0.0, 0.0,
    0.0, 0.0, VIEW_SHRINK, 0.0,
    0.0, 0.0, 0.0, 1.0
);

void main() {
    vec4 linePosStart = ProjMat * VIEW_SCALE * ModelViewMat * vec4(Position, 1.0);
    vec4 linePosEnd = ProjMat * VIEW_SCALE * ModelViewMat * vec4(Position + Normal, 1.0);

    float startW = abs(linePosStart.w) < 0.000001 ? (linePosStart.w < 0.0 ? -0.000001 : 0.000001) : linePosStart.w;
    float endW = abs(linePosEnd.w) < 0.000001 ? (linePosEnd.w < 0.0 ? -0.000001 : 0.000001) : linePosEnd.w;
    vec3 ndc1 = linePosStart.xyz / startW;
    vec3 ndc2 = linePosEnd.xyz / endW;

    vec2 screenSize = max(ScreenSize, vec2(1.0));
    vec2 direction = (ndc2.xy - ndc1.xy) * screenSize;
    float directionLength = length(direction);
    vec2 lineScreenDirection = directionLength > 0.000001 ? direction / directionLength : vec2(1.0, 0.0);
    lineHalfWidth = max(LineWidth * 0.5, 0.5);
    float outerHalfWidth = lineHalfWidth + 1.0;
    vec2 lineOffset = vec2(-lineScreenDirection.y, lineScreenDirection.x)
        * (outerHalfWidth * 2.0) / screenSize;

    if (lineOffset.x < 0.0) {
        lineOffset *= -1.0;
    }

    float side = gl_VertexID % 2 == 0 ? 1.0 : -1.0;
    if (side > 0.0) {
        gl_Position = vec4((ndc1 + vec3(lineOffset, 0.0)) * linePosStart.w, linePosStart.w);
    } else {
        gl_Position = vec4((ndc1 - vec3(lineOffset, 0.0)) * linePosStart.w, linePosStart.w);
    }

    vertexColor = Color;
    edgeDistance = side * outerHalfWidth;
}

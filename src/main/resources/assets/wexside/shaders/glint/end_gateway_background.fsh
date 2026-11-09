#version 330

#moj_import <minecraft:fog.glsl>

in float sphericalVertexDistance;
in float cylindricalVertexDistance;

out vec4 fragColor;

void main() {
    float backgroundAlpha = clamp(float(CUSTOMGLINT_BACKGROUND_VISIBILITY_PERCENT) / 100.0, 0.0, 1.0);
    fragColor = apply_fog(
            vec4(0.0, 0.0, 0.0, backgroundAlpha),
            sphericalVertexDistance,
            cylindricalVertexDistance,
            FogEnvironmentalStart,
            FogEnvironmentalEnd,
            FogRenderDistanceStart,
            FogRenderDistanceEnd,
            FogColor
    );
}

#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:matrix.glsl>
#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;

in vec2 texCoord0;
in float sphericalVertexDistance;
in float cylindricalVertexDistance;

const vec3[] COLORS = vec3[](
    vec3(0.022087, 0.098399, 0.110818),
    vec3(0.011892, 0.095924, 0.089485),
    vec3(0.027636, 0.101689, 0.100326),
    vec3(0.046564, 0.109883, 0.114838),
    vec3(0.064901, 0.117696, 0.097189),
    vec3(0.063761, 0.086895, 0.123646),
    vec3(0.084817, 0.111994, 0.166380),
    vec3(0.097489, 0.154120, 0.091064),
    vec3(0.106152, 0.131144, 0.195191),
    vec3(0.097721, 0.110188, 0.187229),
    vec3(0.133516, 0.138278, 0.148582),
    vec3(0.070006, 0.243332, 0.235792),
    vec3(0.196766, 0.142899, 0.214696),
    vec3(0.047281, 0.315338, 0.321970),
    vec3(0.204675, 0.390010, 0.302066),
    vec3(0.080955, 0.314821, 0.661491)
);

vec2 end_portal_layer_uv(vec2 uv, float layer) {
    float angle = radians((layer * layer * 4321.0 + layer * 9.0) * 2.0);
    mat2 rotate = mat2_rotate_z(angle);
    float zoom = clamp(float(CUSTOMGLINT_ITEM_ZOOM_PERCENT) / 250.0, 0.004, 12.0);
    float scale = ((4.5 - layer / 4.0) * 2.0) * zoom;
    vec2 centered = uv - vec2(0.5);
    vec2 translated = rotate * centered * scale;
    translated += vec2(17.0 / layer, 2.0 + layer / 1.5);
    return fract(translated + vec2(0.5));
}

out vec4 fragColor;

void main() {
    vec3 stars = vec3(0.0);
    for (int i = 0; i < PORTAL_LAYERS; i++) {
        stars += texture(Sampler0, end_portal_layer_uv(texCoord0, float(i + 1))).rgb * COLORS[i];
    }

    float normalizedStrength = clamp(float(CUSTOMGLINT_STRENGTH_PERCENT) / 100.0, 0.0, 1.0);
    float scaledStrength = normalizedStrength * 0.3;
    float starIntensity = mix(0.0, 18.0, pow(scaledStrength, 1.2));
    float starAlpha = clamp(pow(normalizedStrength, 0.65), 0.0, 1.0);
    vec3 color = stars * starIntensity * starAlpha;
    float overlayAlpha = starAlpha;

#if CUSTOMGLINT_BLOOM_MASK == 1
    float strongest = max(max(color.r, color.g), color.b);
    float mask = smoothstep(0.32, 1.30, strongest) * overlayAlpha;
    if (mask < 0.025) {
        discard;
    }
    fragColor = vec4(color * mask, mask);
#else
    fragColor = apply_fog(
            vec4(color, overlayAlpha),
            sphericalVertexDistance,
            cylindricalVertexDistance,
            FogEnvironmentalStart,
            FogEnvironmentalEnd,
            FogRenderDistanceStart,
            FogRenderDistanceEnd,
            FogColor
    );
#endif
}

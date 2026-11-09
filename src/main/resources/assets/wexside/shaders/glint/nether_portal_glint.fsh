#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler1;

in vec2 texCoord0;
in float sphericalVertexDistance;
in float cylindricalVertexDistance;

out vec4 fragColor;

float portal_tick_time() {
    return GameTime * 24000.0;
}

vec2 portal_frame_uv(vec2 uv, float tileScale, float verticalSpeed, float frameIndex) {
    ivec2 textureSizePx = textureSize(Sampler1, 0);
    float frameSize = float(textureSizePx.x);
    float frameCount = max(1.0, floor(float(textureSizePx.y) / frameSize));
    float frameScale = frameSize / float(textureSizePx.y);
    float wrappedFrameIndex = mod(frameIndex, frameCount);
    float seconds = portal_tick_time() / 20.0;
    vec2 tiledUv = fract(uv * tileScale + vec2(seconds * 0.05 + sin(seconds * 2.4 + uv.y * 5.0) * 0.02, -seconds * verticalSpeed));
    return vec2(tiledUv.x, tiledUv.y * frameScale + wrappedFrameIndex * frameScale);
}

vec3 sample_frame(vec2 uv, float tileScale, float verticalSpeed, float frameIndex) {
    return texture(Sampler1, portal_frame_uv(uv, tileScale, verticalSpeed, frameIndex)).rgb;
}

vec3 sample_animated_portal(vec2 uv, float tileScale, float verticalSpeed) {
    ivec2 textureSizePx = textureSize(Sampler1, 0);
    float frameCount = max(1.0, floor(float(textureSizePx.y) / float(textureSizePx.x)));
    float frameCursor = portal_tick_time();
    float frameIndex = floor(mod(frameCursor, frameCount));
    float nextFrameIndex = mod(frameIndex + 1.0, frameCount);
    float frameBlend = fract(frameCursor);
    vec3 current = sample_frame(uv, tileScale, verticalSpeed, frameIndex);
    vec3 next = sample_frame(uv, tileScale, verticalSpeed, nextFrameIndex);
    return mix(current, next, frameBlend);
}

vec3 sample_portal(vec2 uv) {
    vec3 baseLayer = sample_animated_portal(uv, 4.0, 0.16);
    vec3 shimmerLayer = sample_animated_portal(uv + vec2(0.09, 0.05), 4.0, 0.09);
    return mix(baseLayer, shimmerLayer, 0.28);
}

void main() {
    float normalizedStrength = clamp(float(CUSTOMGLINT_STRENGTH_PERCENT) / 100.0, 0.0, 1.0);
    float scaledStrength = normalizedStrength * 0.3;
    float intensity = mix(0.0, 7.0, pow(scaledStrength, 1.15));
    vec3 portalColor = sample_portal(texCoord0);
    vec3 color = portalColor * intensity;

#if CUSTOMGLINT_BLOOM_MASK == 1
    float strongest = max(max(color.r, color.g), color.b);
    float saturation = strongest <= 0.001 ? 0.0 : (strongest - min(min(color.r, color.g), color.b)) / strongest;
    float mask = smoothstep(0.20, 0.90, strongest) * smoothstep(0.10, 0.42, saturation) * normalizedStrength;
    if (mask < 0.025) {
        discard;
    }
    fragColor = vec4(color * mask, mask);
#else
    fragColor = apply_fog(
            vec4(color, 1.0),
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

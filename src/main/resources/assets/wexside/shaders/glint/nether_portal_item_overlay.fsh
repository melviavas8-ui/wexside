#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;

in vec2 texCoord0;
in float sphericalVertexDistance;
in float cylindricalVertexDistance;

out vec4 fragColor;

float portal_tick_time() {
    return GameTime * 24000.0;
}

vec2 portal_frame_uv(vec2 uv, float tileScale, float verticalSpeed, float frameIndex, float motionScale) {
    ivec2 textureSizePx = textureSize(Sampler0, 0);
    float frameSize = float(textureSizePx.x);
    float frameCount = max(1.0, floor(float(textureSizePx.y) / frameSize));
    float frameScale = frameSize / float(textureSizePx.y);
    float wrappedFrameIndex = mod(frameIndex, frameCount);
    float seconds = portal_tick_time() / 20.0;
    vec2 motion = vec2(seconds * 0.05 + sin(seconds * 2.4 + uv.y * 5.0) * 0.02, -seconds * verticalSpeed) * motionScale;
    vec2 tiledUv = fract(uv * tileScale + motion);
    return vec2(tiledUv.x, tiledUv.y * frameScale + wrappedFrameIndex * frameScale);
}

vec3 sample_frame(vec2 uv, float tileScale, float verticalSpeed, float frameIndex, float motionScale) {
    return texture(Sampler0, portal_frame_uv(uv, tileScale, verticalSpeed, frameIndex, motionScale)).rgb;
}

vec3 sample_animated_portal(vec2 uv, float tileScale, float verticalSpeed) {
    ivec2 textureSizePx = textureSize(Sampler0, 0);
    float frameCount = max(1.0, floor(float(textureSizePx.y) / float(textureSizePx.x)));
    float motionScale = clamp(float(CUSTOMGLINT_ITEM_SPEED_PERCENT) / 100.0, 0.0, 2.0);
    float frameCursor = portal_tick_time();
    float frameIndex = floor(mod(frameCursor, frameCount));
    float nextFrameIndex = mod(frameIndex + 1.0, frameCount);
    float frameBlend = fract(frameCursor);
    vec3 current = sample_frame(uv, tileScale, verticalSpeed, frameIndex, motionScale);
    vec3 next = sample_frame(uv, tileScale, verticalSpeed, nextFrameIndex, motionScale);
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
    float intensity = mix(0.0, 8.0, pow(scaledStrength, 1.15));
    float zoomScale = clamp(float(CUSTOMGLINT_ITEM_ZOOM_PERCENT), 1.0, 36.0);
    vec3 portalColor = sample_portal(texCoord0 * zoomScale);
    vec3 color = portalColor * intensity;
    float overlayAlpha = clamp(pow(normalizedStrength, 0.6), 0.0, 1.0);

#if CUSTOMGLINT_BLOOM_MASK == 1
    float strongest = max(max(color.r, color.g), color.b);
    float saturation = strongest <= 0.001 ? 0.0 : (strongest - min(min(color.r, color.g), color.b)) / strongest;
    float mask = smoothstep(0.20, 0.90, strongest) * smoothstep(0.10, 0.42, saturation) * overlayAlpha;
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

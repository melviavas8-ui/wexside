#version 330

uniform sampler2D InSampler;

in vec2 texCoord;
out vec4 fragColor;

float gaussian(float x, float sigma) {
    return exp(-0.5 * (x * x) / max(sigma * sigma, 0.0001));
}

vec3 sample_bloom(vec2 uv) {
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        return vec3(0.0);
    }
    return texture(InSampler, uv).rgb;
}

void main() {
    vec2 texelSize = 1.0 / vec2(textureSize(InSampler, 0));
#if BLOOM_HORIZONTAL == 1
    vec2 direction = vec2(texelSize.x, 0.0);
#else
    vec2 direction = vec2(0.0, texelSize.y);
#endif
    float radius = float(BLOOM_RADIUS_PIXELS);
    float softness = clamp(float(BLOOM_BLUR_PERCENT) / 100.0, 0.0, 4.0);
    float spread = radius * mix(0.28, 1.45, clamp(softness / 4.0, 0.0, 1.0));
    float sigma = max(0.85, spread * mix(0.44, 0.68, clamp(softness / 4.0, 0.0, 1.0)));
    vec3 sum = vec3(0.0);
    float weightSum = 0.0;
    for (int i = -6; i <= 6; i++) {
        float position = float(i) / 6.0;
        float distance = position * spread;
        float weight = gaussian(distance, sigma);
        vec2 uv = texCoord + direction * distance;
        sum += sample_bloom(uv) * weight;
        weightSum += weight;
    }
    fragColor = vec4(sum / max(weightSum, 0.0001), 1.0);
}

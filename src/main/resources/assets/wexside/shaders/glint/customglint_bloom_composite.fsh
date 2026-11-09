#version 330

uniform sampler2D BloomSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 bloomColor = texture(BloomSampler, texCoord).rgb;
    float bloomPeak = max(max(bloomColor.r, bloomColor.g), bloomColor.b);
    float gate = smoothstep(0.006, 0.045, bloomPeak);
    float intensity = float(BLOOM_INTENSITY_PERCENT) / 100.0;
    fragColor = vec4(clamp(bloomColor * gate * intensity, 0.0, 1.0), 1.0);
}

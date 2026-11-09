#version 330

uniform sampler2D Sampler0;

in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec4 sampleColor = texture(Sampler0, texCoord0);
    float strongest = max(max(sampleColor.r, sampleColor.g), sampleColor.b);
    float mask = smoothstep(0.12, 0.72, strongest) * sampleColor.a;
    if (mask < 0.025) {
        discard;
    }
    vec3 color = sampleColor.rgb * mask * mix(1.0, 2.35, clamp(strongest, 0.0, 1.0));
    fragColor = vec4(color, mask);
}

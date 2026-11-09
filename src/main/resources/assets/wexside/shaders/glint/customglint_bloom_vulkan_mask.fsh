#version 330

uniform sampler2D MaskSampler;
uniform sampler2D SceneSampler;

in vec2 texCoord;
out vec4 fragColor;

float saturation_amount(vec3 color) {
    float strongest = max(max(color.r, color.g), color.b);
    float weakest = min(min(color.r, color.g), color.b);
    if (strongest <= 0.001) {
        return 0.0;
    }
    return clamp((strongest - weakest) / strongest, 0.0, 1.0);
}

void main() {
    vec4 maskSample = texture(MaskSampler, texCoord);
    vec3 maskColor = maskSample.rgb;
    float maskPeak = max(max(maskColor.r, maskColor.g), maskColor.b);
    if (maskPeak < 0.01) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 sceneColor = texture(SceneSampler, texCoord).rgb;
    float scenePeak = max(max(sceneColor.r, sceneColor.g), sceneColor.b);
    float sceneSat = saturation_amount(sceneColor);
    float coloredGlow = smoothstep(0.42, 0.82, scenePeak) * smoothstep(0.14, 0.42, sceneSat);
    float whiteGlow = smoothstep(0.72, 0.96, scenePeak) * (1.0 - smoothstep(0.20, 0.42, sceneSat));
    float visibilityGate = max(coloredGlow, whiteGlow * 0.7);
    float sourceGate = smoothstep(0.015, 0.12, maskPeak) * maskSample.a;
    float gate = visibilityGate * sourceGate;

    if (gate < 0.02) {
        fragColor = vec4(0.0);
        return;
    }

    fragColor = vec4(maskColor * gate, gate);
}

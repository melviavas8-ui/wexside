#version 330
in vec4 vertexColor;
noperspective in float edgePx;
noperspective in float halfPx;
out vec4 fragColor;
void main() {
    float d = abs(edgePx);
    float width = abs(halfPx);
    float coverage = clamp(width - d + 0.5, 0.0, 1.0);
    if (coverage <= 0.0) discard;
    float coreW = halfPx < 0.0 ? 0.0 : 1.0 - smoothstep(5.0, 10.0, width);
    float core = exp(-(d * d) / (2.0 * 0.85 * 0.85)) * coreW;
    float halo = pow(clamp(1.0 - d / max(width, 0.001), 0.0, 1.0), 2.2);
    vec3 color = mix(vertexColor.rgb, vec3(1.0), core);
    float alpha = clamp((core * 1.4 + halo * 0.55) * coverage * vertexColor.a, 0.0, 1.0);
    if (alpha <= 0.003) discard;
    fragColor = vec4(color * alpha, 0.0);
}

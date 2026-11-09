#version 330
uniform sampler2D SceneSampler;
in vec3 viewPosition;
in vec4 tint;
in vec2 uv;
in vec2 animation;
out vec4 fragColor;
vec3 safeNormal(vec3 value) {
    return value * inversesqrt(max(dot(value, value), 1.0e-20));
}
void main() {
    float age = animation.x;
    vec3 n = safeNormal(cross(dFdx(viewPosition), dFdy(viewPosition)));
    vec3 eye = safeNormal(-viewPosition);
    if (dot(n, eye) < 0.0) n = -n;
    float fresnel = pow(1.0 - clamp(dot(n, eye), 0.0, 1.0), 3.0);
    float highlight = pow(max(dot(n, safeNormal(eye + normalize(vec3(-0.4, 0.7, 0.5)))), 0.0), 48.0);
    float edgeDistance = min(min(uv.x, uv.y), 1.0 - uv.x - uv.y);
    float edge = 1.0 - smoothstep(0.0, max(fwidth(edgeDistance), 0.004) * 1.5, edgeDistance);
    float cracks = smoothstep(0.13, 0.24, age);
    float fade = 1.0 - smoothstep(0.75, 1.0, age);
    vec2 resolution = vec2(textureSize(SceneSampler, 0));
    vec2 screen = gl_FragCoord.xy / resolution;
    float intact = 1.0 - smoothstep(0.24, 0.34, age);
    vec3 ray = refract(-eye, n, 1.0 / 1.48);
    vec2 offset = (ray.xy - (-eye).xy) * (0.18 * intact + 0.035) / max(length(viewPosition), 0.6);
    offset.x *= resolution.y / resolution.x;
    vec2 sampleUv = clamp(screen + offset, 0.5 / resolution, 1.0 - 0.5 / resolution);
    vec3 background = texture(SceneSampler, sampleUv).rgb;
    float brightness = dot(background, vec3(0.2126, 0.7152, 0.0722));
    float thickness = 1.0 / max(dot(n, eye), 0.25);
    float transmission = exp(-0.22 * thickness);
    vec3 glass = tint.rgb * (0.18 + brightness * transmission * 0.95 + fresnel * 0.28
            + highlight * 0.35 + edge * cracks * 0.24);
    float alpha = (0.68 + fresnel * 0.2 + edge * cracks * 0.1) * smoothstep(0.0, 0.055, age);
    fragColor = vec4(glass, tint.a * alpha * fade);
    if (fragColor.a < 0.005) discard;
}

#version 330

in vec4 tint;
in vec2 animation;
out vec4 fragColor;

void main() {
    vec2 p = vec2(animation.x, animation.y - 5.0);
    float angle = atan(p.y, p.x);
    float boundary = 0.79 + 0.05 * sin(angle * 3.0) + 0.035 * sin(angle * 7.0 + 1.0);
    float radius = length(p);
    float aa = max(fwidth(radius), 0.002);
    float coverage = 1.0 - smoothstep(boundary - aa, boundary + aa, radius);
    if (coverage < 0.005) discard;
    float rim = smoothstep(boundary - 0.13, boundary - 0.025, radius);
    float gloss = exp(-dot((p - vec2(-0.24, -0.3)) * vec2(3.0, 9.0),
                          (p - vec2(-0.24, -0.3)) * vec2(3.0, 9.0)));
    vec3 material = tint.rgb * (0.53 + rim * 0.28) + vec3(0.82, 1.0, 0.9) * gloss * 0.6;
    fragColor = vec4(material, tint.a * coverage * 0.82);
}

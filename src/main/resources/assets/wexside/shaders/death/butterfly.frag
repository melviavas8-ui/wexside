#version 330
in vec4 tint;
in vec2 uv;
in float dissolve;
in vec3 viewPosition;
out vec4 fragColor;
vec3 safeNormal(vec3 value) {
    return value * inversesqrt(max(dot(value, value), 1.0e-20));
}
void main() {
    vec3 normal = safeNormal(cross(dFdx(viewPosition), dFdy(viewPosition)));
    float visibility = 1.0 - smoothstep(0.04, 0.96, dissolve);
    if (uv.x < 0.0) {
        float alpha = tint.a * visibility * visibility;
        fragColor = vec4(tint.rgb * 0.48 * alpha, alpha);
        return;
    }
    vec2 cell = floor(uv * vec2(8.0, 12.0));
    float row = cell.y;
    float width;
    if (row < 1.0) width = 3.0;
    else if (row < 2.0) width = 5.0;
    else if (row < 4.0) width = 6.0;
    else if (row < 5.0) width = 5.0;
    else if (row < 6.0) width = 3.0;
    else if (row < 7.0) width = 5.0;
    else if (row < 9.0) width = 8.0;
    else if (row < 10.0) width = 7.0;
    else if (row < 11.0) width = 5.0;
    else width = 2.0;
    if (cell.x >= width) discard;
    float border = step(width - 1.0, cell.x);
    float vein = step(0.7, fract((cell.x + row * 0.5) / 3.0));
    float spot = step(width - 3.0, cell.x) * (1.0 - border) * step(0.5, mod(row, 3.0));
    vec3 eye = safeNormal(-viewPosition);
    if (dot(normal, eye) < 0.0) normal = -normal;
    float fresnel = pow(1.0 - clamp(dot(normal, eye), 0.0, 1.0), 3.0);
    vec3 light = normalize(vec3(-0.4, 0.8, 0.5));
    float reflection = pow(max(dot(normal, safeNormal(eye + light)), 0.0), 28.0);
    float alpha = (0.24 + fresnel * 0.24 + border * 0.20 + vein * 0.10) * tint.a * visibility;
    vec3 glass = tint.rgb * (0.55 + fresnel * 0.38 + spot * 0.18);
    float glow = border * 0.26 + vein * 0.12 + spot * 0.16 + reflection * 0.48;
    vec3 emission = tint.rgb * glow * tint.a * visibility * visibility;
    fragColor = vec4(glass * alpha + emission, alpha);
    if (fragColor.a < 0.005) discard;
}

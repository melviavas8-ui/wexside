#version 330
in vec4 tint;
in vec3 surfacePos;
in vec3 viewPos;
in vec2 animation;
out vec4 fragColor;

void main() {
    float age = clamp(animation.x, 0.0, 1.0);
    float phase = animation.y * 6.2831853;
    float envelope = smoothstep(0.0, 0.055, age) * (1.0 - smoothstep(0.55, 1.0, age));
    float damping = exp(-age * 2.8);
    float oscillation = age * 15.707963;
    vec3 p = surfacePos;
    float bend = sin(p.x * 3.8 + p.z * 2.6 + phase) * 0.65;
    float wave = sin(p.y * 7.0 + bend - oscillation);
    float crossWave = sin(p.x * 5.0 - p.z * 4.0 + p.y * 2.0 + oscillation * 0.8 + phase);
    float height = (wave + crossWave * 0.35) * damping * 0.045;

    vec3 dx = dFdx(viewPos);
    vec3 dy = dFdy(viewPos);
    vec3 faceCross = cross(dx, dy);
    vec3 normal = faceCross * inversesqrt(max(dot(faceCross, faceCross), 1e-20));
    vec3 view = -viewPos * inversesqrt(max(dot(viewPos, viewPos), 1e-10));
    if (dot(normal, view) < 0.0) normal = -normal;
    vec3 tangentX = cross(dy, normal);
    vec3 tangentY = cross(normal, dx);
    float determinant = dot(dx, tangentX);
    float inverseDet = sign(determinant) / max(abs(determinant), 1e-10);
    vec3 gradient = (tangentX * dFdx(height) + tangentY * dFdy(height)) * inverseDet;
    vec3 wetNormal = normalize(normal - gradient);
    vec3 light = normalize(vec3(-0.45, 0.7, 0.8));
    vec3 halfVector = light + view;
    halfVector *= inversesqrt(max(dot(halfVector, halfVector), 1e-10));
    float ndh = max(dot(wetNormal, halfVector), 0.0);
    float roughness = max(fwidth(ndh), 0.001);
    float broadHighlight = pow(ndh, 12.0);
    float sharpHighlight = smoothstep(0.955 - roughness, 0.99 + roughness, ndh);
    float fresnel = pow(1.0 - clamp(dot(wetNormal, view), 0.0, 1.0), 3.0);
    float thickness = 0.5 + wave * damping * 0.28 + crossWave * damping * 0.12;
    vec3 body = tint.rgb * (0.48 + thickness * 0.45);
    vec3 highlight = mix(tint.rgb, vec3(1.0), 0.22);
    vec3 color = mix(body, highlight, clamp(broadHighlight * 0.5 + sharpHighlight * 0.5 + fresnel * 0.3, 0.0, 1.0));
    float alpha = (0.045 + thickness * 0.04 + broadHighlight * 0.09 + sharpHighlight * 0.14 + fresnel * 0.09)
                * envelope * tint.a;
    fragColor = vec4(clamp(color, 0.0, 1.0), clamp(alpha, 0.0, 0.72));
    if (fragColor.a < 0.003) discard;
}

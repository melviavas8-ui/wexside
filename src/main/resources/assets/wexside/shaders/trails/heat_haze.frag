#version 330 core
uniform sampler2D uScene;
uniform sampler2D uDepth;
uniform vec2 uScreenSize;
uniform float uTime;
uniform float uStrength;
uniform vec4 uColor;
in vec3 vTrail;
in vec2 vAgePulse;
out vec4 fragColor;

vec2 visibleSample(vec2 origin, vec2 offset) {
    vec2 target = clamp(origin + offset, 0.5 / uScreenSize, 1.0 - 0.5 / uScreenSize);
    return texture(uDepth, target).r + 0.000001 < gl_FragCoord.z ? origin : target;
}

void main() {
    vec2 uv = gl_FragCoord.xy / uScreenSize;
    float life = clamp(vTrail.z, 0.0, 1.0);
    float boundary = 1.0 - smoothstep(0.65, 1.0, abs(vTrail.x));
    float alpha = life * boundary;
    if (alpha < 0.002) discard;
    float along = vTrail.y / 2.4 - uTime * 0.42;
    float cell = floor(along);
    vec2 displacement = vec2(0.0);
    float flame = 0.0;
    float glow = 0.0;
    for (int i = -1; i <= 1; i++) {
        float id = cell + float(i);
        vec3 seed = fract(sin(id * vec3(127.1, 269.5, 183.3) + vec3(9.2, 47.1, 81.7)) * 43758.5453);
        float y = along - id - 0.5;
        float direction = seed.x < 0.5 ? -1.0 : 1.0;
        float center = (seed.y - 0.5) * 0.65;
        float bend = direction * sin(y * 4.0 + seed.z * 6.28 + uTime * 0.65);
        float x = vTrail.x - center - bend * 0.12;
        vec2 q = vec2(x / (0.46 + seed.z * 0.2), y / 0.56);
        float cloud = exp(-dot(q, q) * 2.0);
        displacement += vec2(-q.y, q.x) * cloud * direction;
        float tip = smoothstep(-0.35, 0.32, y);
        float tongueX = x - direction * (tip * tip * 0.4 - 0.1);
        float tongueWidth = mix(0.13, 0.025, tip);
        float ends = smoothstep(-0.4, -0.16, y) * (1.0 - smoothstep(0.1, 0.38, y));
        float flicker = 0.7 + 0.3 * sin(uTime * 1.8 + seed.z * 6.28);
        float coreDistance = tongueX / tongueWidth;
        float glowDistance = tongueX / (tongueWidth + 0.14);
        flame += exp(-coreDistance * coreDistance) * ends * flicker;
        glow += exp(-glowDistance * glowDistance) * ends * flicker;
    }
    float resolutionScale = clamp(uScreenSize.y / 1080.0, 0.65, 2.0);
    vec2 offset = displacement * (34.0 * resolutionScale * uStrength * sqrt(life) * boundary) / uScreenSize;
    vec3 color = texture(uScene, visibleSample(uv, offset)).rgb;
    float age = clamp(vAgePulse.x, 0.0, 1.0);
    float colorLife = (1.0 - smoothstep(0.08, 0.65, age)) * uColor.a;
    float pulse = 1.0 + clamp(vAgePulse.y, 0.0, 1.0) * 0.65;
    vec3 emission = uColor.rgb * (flame * 0.95 + glow * 0.3);
    emission += mix(uColor.rgb, vec3(1.0), 0.18) * flame * flame * 0.2;
    fragColor = vec4(color + emission * colorLife * pulse, alpha);
}

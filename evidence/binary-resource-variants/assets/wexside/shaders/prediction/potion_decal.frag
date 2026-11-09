#version 330 core

in vec2 vTexCoord;

uniform sampler2D DepthSampler;

#define MAX_POTIONS 8

#define MAX_AA 0.15

layout(std140) uniform PotionDecal {
    mat4 uInvViewProj;
    vec4 uParams;
    vec4 uPotions[MAX_POTIONS];
    vec4 uColors[MAX_POTIONS];
    vec4 uStyle;
};

out vec4 fragColor;

vec3 reconstruct(vec2 uv, float depth) {
    vec4 ndc = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 world = uInvViewProj * ndc;
    float w = abs(world.w) < 0.000001 ? (world.w < 0.0 ? -0.000001 : 0.000001) : world.w;
    return world.xyz / w;
}

void main() {
    float depth = texture(DepthSampler, vTexCoord).r;

    vec3 world = reconstruct(vTexCoord, depth);
    vec2 worldDx = dFdx(world.xz);
    vec2 worldDy = dFdy(world.xz);

    if (depth >= 1.0) {
        discard;
    }

    int count = clamp(int(uParams.x + 0.5), 0, MAX_POTIONS);
    float fillAlpha = uParams.y;
    float outlineAlpha = uParams.z;
    int style = int(uStyle.x + 0.5);

    vec3 weightedColor = vec3(0.0);
    float weightSum = 0.0;
    float opacity = 0.0;
    float phase = uParams.w / 6.0;
    float waveLife = smoothstep(0.0, 0.16, phase) * (1.0 - smoothstep(0.72, 1.0, phase));
    for (int i = 0; i < MAX_POTIONS; i++) {
        if (i >= count) {
            break;
        }
        vec3 center = uPotions[i].xyz;
        float radius = uPotions[i].w;
        if (radius <= 0.0) continue;
        vec3 delta = world - center;
        float dist = length(delta.xz);
        if (dist > radius + 0.3 || abs(delta.y) > 2.0) {
            continue;
        }
        vec2 radial = delta.xz / max(dist, 0.0001);
        float aa = clamp(abs(dot(radial, worldDx)) + abs(dot(radial, worldDy)), 0.001, MAX_AA);
        vec3 rgb = uColors[i].rgb;
        float r = clamp(dist / radius, 0.0, 1.0);
        float fill = 1.0 - smoothstep(radius - aa, radius + aa, dist);
        float rimWidth = clamp(radius * 0.009, 0.018, 0.045);
        float outline = 1.0 - smoothstep(rimWidth, rimWidth + aa, abs(dist - radius));
        float innerGlow = exp(-max(radius - dist, 0.0) / max(radius * 0.08, 0.04)) * fill;
        float waveRadius = radius * mix(0.12, 0.97, phase);
        float waveWidth = max(radius * 0.025, aa);
        float waveDistance = (dist - waveRadius) / waveWidth;
        float wave = exp(-waveDistance * waveDistance) * waveLife * fill;
        float heightFade = 1.0 - smoothstep(1.15, 2.0, abs(delta.y));
        float base = fillAlpha * fill * (0.18 + 0.32 * r * r);
        float a = max(base + fillAlpha * (innerGlow * 0.28 + wave * 0.22), outlineAlpha * outline);
        if (style == 1) {
            a = max(fillAlpha * fill, outlineAlpha * outline);
        } else if (style == 2) {
            vec2 p = delta.xz / radius;
            float t = phase * 6.2831853;
            vec2 drift = vec2(cos(t), sin(t)) * 0.38;
            vec2 q = p * 8.0 + drift;
            q += 0.65 * sin(q.yx * 0.72 + vec2(sin(t), cos(t)));
            float field = sin(q.x + sin(q.y * 0.85)) + sin(q.y - cos(q.x * 0.7));
            float pixelWidth = clamp((length(worldDx) + length(worldDy)) / radius * 10.0, 0.035, 1.6);
            float veins = 1.0 - smoothstep(0.08, 0.16 + pixelWidth, abs(field));
            float halo = exp(-abs(field) * 2.5);
            float detailFade = 1.0 - smoothstep(0.6, 1.6, pixelWidth);
            a = max(fillAlpha * fill * (0.10 + detailFade * (veins * 0.95 + halo * 0.28)
                    + innerGlow * 0.20), outlineAlpha * outline * 0.8);
        } else if (style == 3) {
            vec2 p = delta.xz / radius;
            float t = phase * 6.2831853;
            vec2 drift = vec2(sin(t), cos(t)) * 0.45;
            vec2 q = p * 3.6;
            q += vec2(sin(q.y * 0.9 + drift.x), cos(q.x * 0.8 + drift.y)) * 1.2;
            float flow = sin(q.x + q.y * 0.65 + drift.x)
                    + 0.5 * sin(q.y * 1.8 - q.x * 0.45 + drift.y);
            float mist = exp(-flow * flow * 2.8);
            float filament = exp(-flow * flow * 24.0);
            float envelope = 0.65 + 0.35 * sin(q.y * 0.7 - q.x * 0.4 + drift.y);
            float detailFade = 1.0 - smoothstep(0.04, 0.18,
                    (length(worldDx) + length(worldDy)) / radius);
            a = max(fillAlpha * fill * (0.08 + mist * envelope * 0.72
                    + filament * detailFade * 0.30 + innerGlow * 0.15), outlineAlpha * outline * 0.7);
        }
        a = clamp(a * heightFade * uColors[i].a, 0.0, 1.0);
        float weight = a * a;
        weightedColor += rgb * weight;
        weightSum += weight;
        opacity = max(opacity, a);
    }
    if (opacity <= 0.002) {
        discard;
    }
    fragColor = vec4(weightedColor / max(weightSum, 0.000001), opacity);
}

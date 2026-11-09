#version 330 core

in vec2 vTextureUV;

uniform sampler2D uMaskTexture;
uniform sampler2D uSceneTexture;
uniform sampler2D uMaskDepthTexture;
uniform int uMode;
uniform vec2 uScreenSize;
uniform vec4 uColor;
uniform vec4 uColor2;
uniform int uGradient;
uniform vec2 uLocation;
uniform vec2 uRectSize;
uniform float uTime;
uniform float uRadius;

out vec4 FragColor;

#define TAU 6.28318530718
#define PI 3.14159265359
#define NOISE 0.5/255.0

const vec2 CENTER = vec2(0.5);
const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);
const float N = 1.0 / 7.0;
const vec3 LUM_WEIGHTS = vec3(0.299, 0.587, 0.114);

float saturate(float value)
{
    return clamp(value, 0.0, 1.0);
}

vec2 screenUv()
{
    return clamp(gl_FragCoord.xy / max(uScreenSize, vec2(1.0)), vec2(0.001), vec2(0.999));
}

vec2 localPlayerUv()
{
    vec2 local = (gl_FragCoord.xy - uLocation) / max(uRectSize, vec2(1.0));
    return clamp(local, vec2(0.0), vec2(1.0));
}

bool insideRect()
{
    vec2 rectMin = uLocation;
    vec2 rectMax = uLocation + max(uRectSize, vec2(1.0));

    return gl_FragCoord.x >= rectMin.x
            && gl_FragCoord.x <= rectMax.x
            && gl_FragCoord.y >= rectMin.y
            && gl_FragCoord.y <= rectMax.y;
}

vec4 scene(vec2 uv)
{
    return texture(uSceneTexture, clamp(uv, vec2(0.001), vec2(0.999)));
}

vec4 mask(vec2 uv)
{
    return texture(uMaskTexture, clamp(uv, vec2(0.001), vec2(0.999)));
}

float maskDepth(vec2 uv)
{
    return texture(uMaskDepthTexture, clamp(uv, vec2(0.001), vec2(0.999))).r;
}

vec4 applyColorWithNoise(vec2 coords, vec4 color)
{
    vec4 result = color;
    result.rgb += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
    return result;
}

vec4 tintColor(vec2 localUv)
{
    float t = uGradient == 1 ? smoothstep(0.0, 1.0, localUv.y) : 0.0;
    return mix(uColor, uColor2, t);
}

vec4 permute(vec4 x)
{
    return mod(((x * 34.0) + 1.0) * x, 289.0);
}

vec4 taylorInvSqrt(vec4 r)
{
    return 1.79284291400159 - 0.85373472095314 * r;
}

float snoise(vec3 v)
{
    vec3 i = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);

    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);

    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + 2.0 * C.xxx;
    vec3 x3 = x0 - 1.0 + 3.0 * C.xxx;

    i = mod(i, 289.0);
    vec4 p = permute(permute(permute(
            i.z + vec4(0.0, i1.z, i2.z, 1.0)) +
            i.y + vec4(0.0, i1.y, i2.y, 1.0)) +
            i.x + vec4(0.0, i1.x, i2.x, 1.0));

    vec3 ns = N * D.wyz - D.xzx;
    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_);

    vec4 x = x_ * ns.x + ns.yyyy;
    vec4 y = y_ * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);

    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);
    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));

    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;

    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);

    vec4 norm = taylorInvSqrt(vec4(
            dot(p0, p0), dot(p1, p1),
            dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;

    vec4 m = max(0.6 - vec4(
            dot(x0, x0), dot(x1, x1),
            dot(x2, x2), dot(x3, x3)), 0.0);
    m = m * m;

    return 42.0 * dot(m * m, vec4(
            dot(p0, x0), dot(p1, x1),
            dot(p2, x2), dot(p3, x3)));
}

vec4 blurScene(vec2 uv, float blurAmount)
{
    vec2 radius = vec2(blurAmount) / max(uScreenSize, vec2(1.0));
    vec4 pixelColor = scene(uv) * 4.0;
    float totalWeight = 4.0;
    float blurDirections = 4.0;

    for (float d = 0.0; d < TAU; d += TAU / blurDirections)
    {
        vec2 dir = vec2(cos(d), sin(d));
        vec2 sampleUv = clamp(uv + dir * radius * 0.5, vec2(0.001), vec2(0.999));
        pixelColor += scene(sampleUv) * 1.5;
        totalWeight += 1.5;
    }

    return pixelColor / totalWeight;
}

vec3 calculateNormalFromMask(vec2 sampleUv, vec2 localUv)
{
    vec2 stepSize = 1.5 / max(uScreenSize, vec2(1.0));

    vec4 texL = mask(sampleUv + vec2(-stepSize.x, 0.0));
    vec4 texR = mask(sampleUv + vec2(stepSize.x, 0.0));
    vec4 texD = mask(sampleUv + vec2(0.0, -stepSize.y));
    vec4 texU = mask(sampleUv + vec2(0.0, stepSize.y));

    float hL = dot(texL.rgb, LUM_WEIGHTS) * 0.6 + texL.a * 0.4;
    float hR = dot(texR.rgb, LUM_WEIGHTS) * 0.6 + texR.a * 0.4;
    float hD = dot(texD.rgb, LUM_WEIGHTS) * 0.6 + texD.a * 0.4;
    float hU = dot(texU.rgb, LUM_WEIGHTS) * 0.6 + texU.a * 0.4;

    float dx = (hR - hL) * 4.0;
    float dy = (hU - hD) * 4.0;

    vec2 toCenter = CENTER - localUv;
    vec3 baseNormal = vec3(-dx, -dy, 1.0);
    vec3 positionNormal = normalize(vec3(toCenter * 1.5, 1.0));

    return normalize(mix(baseNormal, positionNormal, 0.3));
}

vec3 calculateReflectionVector(vec2 uv, vec3 normal, vec2 localUv)
{
    vec2 ndc = (uv * 2.0) - 1.0;
    vec3 viewDir = normalize(vec3(ndc, -1.0));
    vec3 reflectDir = viewDir - 2.0 * dot(viewDir, normal) * normal;

    vec2 toCenter = CENTER - localUv;
    reflectDir += normal * length(toCenter) * 0.1;

    return normalize(reflectDir);
}

float fresnel(vec3 viewDir, vec3 normal, float power)
{
    return pow(1.0 - max(dot(viewDir, normal), 0.0), power);
}

vec4 sampleEnvironment(vec3 reflectDir)
{
    vec3 absDir = abs(reflectDir);
    float maxAxis = max(max(absDir.x, absDir.y), absDir.z);

    vec2 uv;
    if (absDir.x == maxAxis)
    {
        uv = reflectDir.x > 0.0
                ? vec2(-reflectDir.z, -reflectDir.y) / absDir.x
                : vec2(reflectDir.z, -reflectDir.y) / absDir.x;
    }
    else if (absDir.y == maxAxis)
    {
        uv = reflectDir.y > 0.0
                ? vec2(reflectDir.x, reflectDir.z) / absDir.y
                : vec2(reflectDir.x, -reflectDir.z) / absDir.y;
    }
    else
    {
        uv = reflectDir.z > 0.0
                ? vec2(reflectDir.x, -reflectDir.y) / absDir.z
                : vec2(-reflectDir.x, -reflectDir.y) / absDir.z;
    }

    uv = uv * 0.5 + 0.5;
    uv.y = 1.0 - uv.y;

    return scene(uv);
}

vec4 renderFilled(vec2 localUv, vec4 baseSample)
{
    vec4 tint = tintColor(localUv);
    vec4 finalColor = applyColorWithNoise(localUv, tint);
    finalColor.rgb = mix(finalColor.rgb, baseSample.rgb, 1.0);
    finalColor.rgb = mix(finalColor.rgb, tint.rgb, tint.a);
    return vec4(finalColor.rgb, baseSample.a);
}

float filledOutlineAlpha(vec2 uv, vec4 baseSample)
{
    if (baseSample.a > 0.01)
    {
        return 0.0;
    }

    vec2 texelSize = 1.0 / max(uScreenSize, vec2(1.0));

    vec4 leftMask = mask(uv + vec2(-texelSize.x, 0.0));
    vec4 rightMask = mask(uv + vec2(texelSize.x, 0.0));
    vec4 downMask = mask(uv + vec2(0.0, -texelSize.y));
    vec4 upMask = mask(uv + vec2(0.0, texelSize.y));
    vec4 downLeftMask = mask(uv + vec2(-texelSize.x, -texelSize.y));
    vec4 downRightMask = mask(uv + vec2(texelSize.x, -texelSize.y));
    vec4 upLeftMask = mask(uv + vec2(-texelSize.x, texelSize.y));
    vec4 upRightMask = mask(uv + vec2(texelSize.x, texelSize.y));

    float neighborAlpha = max(
            max(max(leftMask.a, rightMask.a), max(downMask.a, upMask.a)),
            max(max(downLeftMask.a, downRightMask.a), max(upLeftMask.a, upRightMask.a))
    );

    return smoothstep(0.02, 0.70, neighborAlpha);
}

vec4 renderGlass(vec2 uv, vec2 localUv, vec4 baseSample)
{
    vec4 tint = tintColor(localUv);
    vec3 normal = calculateNormalFromMask(uv, localUv);
    float noise = snoise(vec3(localUv, 1.0));
    vec3 distortedNormal = normalize(normal + vec3(noise * 0.015 * 0.08, noise * 0.015 * 0.08, 0.0));

    vec3 reflectDir = calculateReflectionVector(uv, distortedNormal, localUv);
    vec4 envColor = sampleEnvironment(reflectDir);

    vec2 reflectedUv = vec2(uv.x, 1.0 - uv.y) + reflectDir.xy * 0.03;
    vec4 blurredEnv = blurScene(reflectedUv, 10.0);

    float blurMix = smoothstep(0.3, 0.7, 10.0 / 12.0);
    vec4 reflectionColor = mix(envColor, blurredEnv, blurMix);

    vec2 ndc = (uv * 2.0) - 1.0;
    vec3 viewDir = normalize(vec3(ndc, -1.0));
    float fresnelFactor = fresnel(viewDir, distortedNormal, 2.0);

    float reflectionStrength = 0.75 + fresnelFactor * 0.25;
    vec3 finalColor = mix(baseSample.rgb * tint.rgb, reflectionColor.rgb * tint.rgb, reflectionStrength);

    float finalAlpha = tint.a * baseSample.a * (0.85 + fresnelFactor * 0.15);
    return vec4(finalColor, finalAlpha);
}

vec3 materialHash(vec2 cell)
{
    vec3 p = fract(vec3(cell.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.xxy + p.yzz) * p.zyx);
}

vec3 materialNormal(vec2 uv)
{
    vec2 texel = 1.0 / max(uScreenSize, vec2(1.0));
    float center = maskDepth(uv);
    float left = maskDepth(uv - vec2(texel.x, 0));
    float right = maskDepth(uv + vec2(texel.x, 0));
    float down = maskDepth(uv - vec2(0, texel.y));
    float up = maskDepth(uv + vec2(0, texel.y));
    float dx = abs(right - center) < abs(center - left) ? right - center : center - left;
    float dy = abs(up - center) < abs(center - down) ? up - center : center - down;
    return normalize(vec3(clamp(-vec2(dx,dy) * uScreenSize * 18.0, vec2(-3), vec2(3)), 1.0));
}

vec2 materialUv(vec2 localUv)
{
    return (localUv - 0.5) * vec2(uRectSize.x / max(uRectSize.y, 1.0), 1.0)
           * 4.5 / max(uRadius, 0.6);
}

vec4 renderBubble(vec2 uv, vec2 localUv, vec4 baseSample)
{
    vec4 tint = tintColor(localUv);
    vec2 p = materialUv(localUv);
    float time = uTime * 0.7;
    vec2 flow = p * 1.2 - vec2(0.0, time * 0.12);
    flow += vec2(snoise(vec3(p * 1.1, time * 0.12)), snoise(vec3(p * 1.1 + 7.0, time * 0.12))) * 0.18;
    vec2 cell = floor(flow);
    float field = 2.0;
    vec3 bubbleNormal = vec3(0,0,1);
    float aa = max(length(fwidth(flow)), 0.003);
    for (int y = -1; y <= 1; ++y) for (int x = -1; x <= 1; ++x) {
        vec2 id = cell + vec2(x,y);
        vec3 random = materialHash(id);
        vec2 center = id + 0.25 + random.xy * 0.5
                    + 0.12 * sin(vec2(time * 0.31, time * 0.23) + random.xy * TAU);
        float radius = 0.38 + random.z * 0.24;
        vec2 delta = flow - center;
        float distanceToBubble = length(delta) - radius;
        vec2 sphere = delta / radius;
        vec3 candidate = normalize(vec3(sphere, sqrt(max(0.035, 1.0 - dot(sphere,sphere)))));
        float blend = clamp(0.5 + 0.5 * (field - distanceToBubble) / 0.28, 0.0, 1.0);
        bubbleNormal = normalize(mix(bubbleNormal, candidate, blend));
        field = mix(field, distanceToBubble, blend) - 0.28 * blend * (1.0 - blend);
    }
    float shell = exp(-abs(field) / max(aa, 0.018));
    float interior = 1.0 - smoothstep(-aa, aa, field);
    vec3 surface = materialNormal(uv);
    vec3 normal = normalize(mix(surface, normalize(bubbleNormal + surface * 0.85), interior * 0.72));
    float flowNoise = snoise(vec3(p * 0.85, time * 0.08));
    float film = snoise(vec3(p * 2.7 + flowNoise, time * 0.15));
    float facing = clamp(normal.z, 0.0, 1.0);
    float thickness = smoothstep(-0.6, 0.7, film * 0.6 + flowNoise * 0.4);
    vec2 aspect = vec2(uScreenSize.y / max(uScreenSize.x, 1.0), 1.0);
    vec2 refracted = uv + (surface.xy * 0.002
                    + bubbleNormal.xy * interior * (0.006 + shell * 0.003)) * aspect;
    vec3 background = scene(refracted).rgb;
    float specular = pow(max(dot(normal, normalize(vec3(-0.45,0.65,1.0))), 0.0), 38.0) * interior;
    float softReflection = pow(max(dot(normal, normalize(vec3(0.65,-0.25,0.8))), 0.0), 18.0) * interior;
    float grazing = pow(1.0 - facing, 2.5);
    float lighting = dot(bubbleNormal.xy, normalize(vec2(-0.6,0.8)));
    float crescent = pow(max(lighting, 0.0), 2.0);
    float shadow = pow(max(-lighting, 0.0), 2.0) * interior;
    vec3 filmColor = mix(tint.rgb, tintColor(localUv + vec2(0,0.15)).rgb, thickness * 0.35);
    vec3 bodyColor = mix(baseSample.rgb, baseSample.rgb * tint.rgb, 0.26);
    vec3 color = mix(bodyColor, background * mix(vec3(1.0), tint.rgb, 0.18), 0.42)
               * (0.90 + thickness * 0.10 - grazing * 0.06 - shadow * 0.18)
               + filmColor * (0.025 + thickness * 0.055);
    vec3 highlight = mix(filmColor, vec3(1.0), 0.72);
    float reflection = specular * 0.48 + softReflection * 0.12
                     + shell * crescent * 0.28;
    color += highlight * reflection * (1.0 - clamp(color, 0.0, 1.0) * 0.75);
    return vec4(color, baseSample.a * tint.a * 0.92);
}

float galaxyStars(vec2 p, float layer, float time)
{
    vec2 grid = p * (10.0 + layer * 7.0);
    vec2 cell = floor(grid);
    float stars = 0.0;
    float pixel = max(length(fwidth(grid)), 0.008);
    for (int y = -1; y <= 1; ++y) for (int x = -1; x <= 1; ++x) {
        vec2 id = cell + vec2(x,y);
        vec3 random = materialHash(id + layer * 31.7);
        vec2 delta = grid - id - (0.15 + random.xy * 0.7);
        float size = mix(0.022, 0.055, random.z);
        float point = 1.0 - smoothstep(size, size + pixel, length(delta));
        float energy = size * size / max((size + pixel) * (size + pixel), 0.0001);
        float glow = exp(-dot(delta,delta) * 110.0) * 0.13;
        float flare = (exp(-abs(delta.x) * 110.0 - abs(delta.y) * 15.0)
                     + exp(-abs(delta.y) * 110.0 - abs(delta.x) * 15.0)) * step(0.992, random.z) * 0.35;
        stars += (point * sqrt(energy) + glow + flare) * step(0.92, random.z)
               * (0.78 + 0.22 * sin(time * 0.7 + random.x * TAU));
    }
    return stars;
}

vec4 renderGalaxy(vec2 uv, vec2 localUv, vec4 baseSample)
{
    vec4 tint = tintColor(localUv);
    vec2 p = materialUv(localUv);
    float time = uTime * 0.7;
    vec2 parallax = (uv - 0.5) * 0.15;
    vec3 color = vec3(0.004,0.006,0.013);
    vec3 secondary = uGradient == 1 ? uColor2.rgb : mix(tint.brg, vec3(0.95,0.30,0.12), 0.3);
    float transmission = 1.0;
    for (int i = 0; i < 6; ++i) {
        float layer = float(i);
        float depth = layer * 0.16;
        vec2 q = p + parallax * depth + vec2(depth * 0.23, -depth * 0.17);
        q += vec2(sin(p.y * 1.3 + time * 0.55 + depth), cos(p.x * 1.1 - time * 0.43 + depth)) * 0.22;
        q += vec2(-time * (0.09 + depth * 0.035), time * 0.045);
        float warp = snoise(vec3(q * 1.05 + vec2(time * 0.10, -time * 0.07), depth + 2.0 + time * 0.08));
        float flow = snoise(vec3(q * 3.1 + warp * 0.8 - vec2(time * 0.24,0), depth * 1.8));
        float dust = snoise(vec3(q * 8.5 + flow * 0.45, depth * 3.0 + time * 0.20));
        float fissure = sin(q.x * 2.1 + q.y * 1.35 + warp * 2.2 + depth * 0.6);
        float ridge = abs(fissure);
        float hotEdge = exp(-abs(ridge - 0.20) * 28.0);
        float haze = exp(-ridge * ridge * 3.8) * smoothstep(-0.35, 0.65, flow);
        float threads = pow(max(0.0, 1.0 - abs(flow + dust * 0.28)), 9.0);
        float cavity = 1.0 - smoothstep(0.035, 0.17, ridge);
        float density = haze * (0.25 + threads * 0.8) * (1.0 - cavity * 0.92);
        vec3 gas = mix(tint.rgb, secondary, 0.15 + 0.5 * smoothstep(-0.5,0.7, flow + depth * 0.6));
        vec3 emission = gas * density * 0.82 + mix(gas, vec3(1.0,0.91,0.78), 0.32)
                      * hotEdge * (0.28 + threads * 0.65) * 0.52;
        float surge = pow(max(0.0, sin(q.x * 2.8 - q.y * 2.1 - time * 2.2 + depth * 1.5)), 8.0);
        emission += mix(gas, tint.rgb, 0.55) * hotEdge * surge * (0.65 + threads * 0.6);
        color += emission * transmission;
        transmission *= 1.0 - density * 0.20 - cavity * 0.09;
    }
    float stars = galaxyStars(p + parallax * 0.6 + vec2(time * 0.06,-time * 0.025), 0.0, time);
    stars += galaxyStars(p * 1.25 + parallax * 1.7 - vec2(time * 0.03,time * 0.085), 1.0, time) * 0.5;
    color += mix(tint.rgb, vec3(0.95,0.98,1.0), 0.85) * stars;
    color = color / (1.0 + color * 0.38);
    return vec4(color, baseSample.a * tint.a);
}

void main()
{
    if (!insideRect())
    {
        discard;
    }

    vec2 uv = screenUv();
    vec2 localUv = localPlayerUv();
    vec4 baseSample = mask(uv);

    if (uMode == 1)
    {
        float outlineAlpha = filledOutlineAlpha(uv, baseSample);
        if (outlineAlpha > 0.01)
        {
            vec4 outlineTint = tintColor(localUv);
            FragColor = vec4(outlineTint.rgb, outlineTint.a * outlineAlpha);
            return;
        }

        if (baseSample.a <= 0.01)
        {
            discard;
        }

        FragColor = renderFilled(localUv, baseSample);
        return;
    }

    if (baseSample.a <= 0.01)
    {
        discard;
    }

    if (uMode == 5) {
        FragColor = renderBubble(uv, localUv, baseSample);
        return;
    }
    if (uMode == 6) {
        FragColor = renderGalaxy(uv, localUv, baseSample);
        return;
    }
    FragColor = renderGlass(uv, localUv, baseSample);
}

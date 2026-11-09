#version 330 core

in vec2 vTextureUV;

uniform sampler2D uMaskTexture;
uniform sampler2D uSceneTexture;
uniform sampler2D uMaskDepthTexture;
uniform sampler2D uSceneDepthTexture;
uniform int uPreview;
uniform int uMode;
uniform vec2 uScreenSize;
uniform vec4 uColor;
uniform vec4 uHiddenColor;
uniform int uVisibleEnabled;
uniform int uHiddenEnabled;
uniform vec2 uLocation;
uniform vec2 uRectSize;
uniform float uTime;

out vec4 FragColor;

#define TAU 6.28318530718
#define PI 3.14159265359
#define NOISE 0.5/255.0
#define DEPTH_BIAS 0.0001

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

vec4 maskTex(vec2 uv)
{
    return texture(uMaskTexture, clamp(uv, vec2(0.001), vec2(0.999)));
}

float maskDepth(vec2 uv)
{
    return texture(uMaskDepthTexture, clamp(uv, vec2(0.001), vec2(0.999))).r;
}

float sceneDepth(vec2 uv)
{
    return texture(uSceneDepthTexture, clamp(uv, vec2(0.001), vec2(0.999))).r;
}

bool isPixelVisible(vec2 uv)
{
    if (uPreview != 0) return true;
    float md = maskDepth(uv);
    float sd = sceneDepth(uv);
    return md <= sd - DEPTH_BIAS || sd >= 0.9999;
}

vec4 applyColorWithNoise(vec2 coords, vec4 color)
{
    vec4 result = color;
    result.rgb += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
    return result;
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

    vec4 texL = maskTex(sampleUv + vec2(-stepSize.x, 0.0));
    vec4 texR = maskTex(sampleUv + vec2(stepSize.x, 0.0));
    vec4 texD = maskTex(sampleUv + vec2(0.0, -stepSize.y));
    vec4 texU = maskTex(sampleUv + vec2(0.0, stepSize.y));

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






vec4 renderSimple(vec2 localUv, vec4 baseSample, vec4 col)
{
    vec4 finalColor = applyColorWithNoise(localUv, col);
    return vec4(finalColor.rgb, finalColor.a * baseSample.a);
}

vec4 renderFilled(vec2 localUv, vec4 baseSample, vec4 col)
{
    vec4 finalColor = applyColorWithNoise(localUv, col);
    finalColor.rgb = mix(finalColor.rgb, baseSample.rgb, 1.0);
    finalColor.rgb = mix(finalColor.rgb, col.rgb, col.a);
    return vec4(finalColor.rgb, baseSample.a);
}

float filledOutlineAlpha(vec2 uv, vec4 baseSample)
{
    vec2 texelSize = 1.0 / max(uScreenSize, vec2(1.0));
    float centerDepth = maskDepth(uv);

    vec4 leftMask = maskTex(uv + vec2(-texelSize.x, 0.0));
    vec4 rightMask = maskTex(uv + vec2(texelSize.x, 0.0));
    vec4 downMask = maskTex(uv + vec2(0.0, -texelSize.y));
    vec4 upMask = maskTex(uv + vec2(0.0, texelSize.y));

    float silhouetteEdge = max(
            max(abs(baseSample.a - leftMask.a), abs(baseSample.a - rightMask.a)),
            max(abs(baseSample.a - downMask.a), abs(baseSample.a - upMask.a))
    );

    float leftDepth = leftMask.a > 0.01 ? maskDepth(uv + vec2(-texelSize.x, 0.0)) : centerDepth;
    float rightDepth = rightMask.a > 0.01 ? maskDepth(uv + vec2(texelSize.x, 0.0)) : centerDepth;
    float downDepth = downMask.a > 0.01 ? maskDepth(uv + vec2(0.0, -texelSize.y)) : centerDepth;
    float upDepth = upMask.a > 0.01 ? maskDepth(uv + vec2(0.0, texelSize.y)) : centerDepth;

    float depthEdge = max(
            max(abs(centerDepth - leftDepth), abs(centerDepth - rightDepth)),
            max(abs(centerDepth - downDepth), abs(centerDepth - upDepth))
    );

    float colorEdge = max(
            max(length(baseSample.rgb - leftMask.rgb), length(baseSample.rgb - rightMask.rgb)),
            max(length(baseSample.rgb - downMask.rgb), length(baseSample.rgb - upMask.rgb))
    );

    float silhouetteFactor = step(0.10, silhouetteEdge);
    float depthFactor = smoothstep(0.0015, 0.01, depthEdge);
    float colorFactor = smoothstep(0.18, 0.55, colorEdge) * step(0.02, baseSample.a);

    return max(silhouetteFactor, max(depthFactor, colorFactor));
}

bool outlineNeighborVisibility(vec2 uv)
{
    vec2 texelSize = 1.0 / max(uScreenSize, vec2(1.0));
    vec2 offsets[4] = vec2[](
            vec2(-texelSize.x, 0.0),
            vec2(texelSize.x, 0.0),
            vec2(0.0, -texelSize.y),
            vec2(0.0, texelSize.y)
    );
    for (int i = 0; i < 4; i++)
    {
        vec2 neighborUv = uv + offsets[i];
        if (maskTex(neighborUv).a > 0.01 && isPixelVisible(neighborUv))
        {
            return true;
        }
    }
    return false;
}

vec4 renderGlass(vec2 uv, vec2 localUv, vec4 baseSample, vec4 col)
{
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
    vec3 finalColor = mix(baseSample.rgb * col.rgb, reflectionColor.rgb * col.rgb, reflectionStrength);

    float finalAlpha = col.a * baseSample.a * (0.85 + fresnelFactor * 0.15);
    return vec4(finalColor, finalAlpha);
}



void main()
{
    if (!insideRect())
    {
        discard;
    }

    vec2 uv = screenUv();
    vec2 localUv = localPlayerUv();
    vec4 baseSample = maskTex(uv);

    if (uMode != 1 && baseSample.a <= 0.01)
    {
        discard;
    }

    bool sameAppearance = uVisibleEnabled != 0 && uHiddenEnabled != 0 && all(equal(uColor, uHiddenColor));
    bool visible = sameAppearance || isPixelVisible(uv);
    // Обводка Filled обрабатывается ДО общих discard'ов: её внешние пиксели классифицируются
    // по соседнему пикселю модели, фоновая видимость для них не имеет смысла
    if (uMode == 1)
    {
        float outlineAlpha = filledOutlineAlpha(uv, baseSample);
        if (outlineAlpha > 0.01)
        {
            bool outlineVisible = sameAppearance || (baseSample.a > 0.01 ? visible : outlineNeighborVisibility(uv));
            if (outlineVisible && uVisibleEnabled == 0)
            {
                discard;
            }
            if (!outlineVisible && uHiddenEnabled == 0)
            {
                discard;
            }
            vec4 outlineColor = outlineVisible ? uColor : uHiddenColor;
            FragColor = vec4(outlineColor.rgb, 1.0);
            return;
        }
    }

    if (visible && uVisibleEnabled == 0)
    {
        discard;
    }
    if (!visible && uHiddenEnabled == 0)
    {
        discard;
    }

    vec4 effColor = visible ? uColor : uHiddenColor;

    if (uMode == 0)
    {
        if (baseSample.a <= 0.01)
        {
            discard;
        }

        FragColor = renderSimple(localUv, baseSample, effColor);
        return;
    }

    if (uMode == 1)
    {
        if (baseSample.a <= 0.01)
        {
            discard;
        }

        FragColor = renderFilled(localUv, baseSample, effColor);
        return;
    }

    if (baseSample.a <= 0.01)
    {
        discard;
    }

    FragColor = renderGlass(uv, localUv, baseSample, effColor);
}

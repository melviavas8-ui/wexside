#version 150

in vec2 texCoord;
out vec4 fragColor;
bool skyOccluded;

uniform sampler2D DepthSampler;

layout(std140) uniform AmbientSkyData {
    vec4 meta;
    vec4 skyColor;
    vec4 skyColor2;
    vec4 params;
    vec4 cameraRight;
    vec4 cameraUp;
    vec4 cameraForward;
};

bool skyHidden(vec2 uv) {
    vec2 margin = vec2(1.5 / max(meta.x, 1.0), 1.5 / max(meta.y, 1.0));
    float far = texture(DepthSampler, uv + vec2(-margin.x, -margin.y)).r;
    far = max(far, texture(DepthSampler, uv + vec2(margin.x, -margin.y)).r);
    far = max(far, texture(DepthSampler, uv + vec2(-margin.x, margin.y)).r);
    far = max(far, texture(DepthSampler, uv + vec2(margin.x, margin.y)).r);
    return far < 1.0;
}

vec3 skyDirection(vec2 uv) {
    vec2 ndc = uv * 2.0 - 1.0;
    float tanHalfFov = max(params.z, 0.01);
    ndc.x *= (meta.x / max(meta.y, 1.0)) * tanHalfFov;
    ndc.y *= tanHalfFov;
    return normalize(cameraForward.xyz + cameraRight.xyz * ndc.x + cameraUp.xyz * ndc.y);
}

vec3 tintColor(vec2 uv) {
    float gradientT = params.x > 0.5 ? smoothstep(0.0, 1.0, uv.y) : 0.0;
    return mix(skyColor.rgb, skyColor2.rgb, gradientT);
}

float verticalFade(vec2 uv) {
    float lower = smoothstep(0.02, 0.34, uv.y);
    float upper = 1.0 - smoothstep(0.97, 1.0, uv.y) * 0.12;
    return lower * upper;
}

mat2 rotate2(float a) {
    float s = sin(a);
    float c = cos(a);
    return mat2(c, -s, s, c);
}

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float valueNoise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);

    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));

    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm2(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;

    for (int i = 0; i < 5; i++) {
        value += valueNoise2(p) * amplitude;
        p = rotate2(0.58) * p * 2.02 + vec2(17.13, 8.47);
        amplitude *= 0.5;
    }

    return value;
}

vec3 waterEffect(vec2 uv, float time, vec3 tint, out float alphaEnergy) {
    vec2 p = uv * 8.0 - vec2(20.0);
    vec2 i = p;
    float c = 1.0;
    float inten = 0.05;

    for (int n = 0; n < 10; n++) {
        float t = time * (1.0 - (3.0 / float(n + 1)));
        i = p + vec2(cos(t - i.x) + sin(t + i.y), sin(t - i.y) + cos(t + i.x));
        vec2 wave = max(abs(vec2(sin(i.x + t), cos(i.y + t))), vec2(0.015)) / inten;
        c += 1.0 / max(length(vec2(p.x / wave.x, p.y / wave.y)), 0.001);
    }

    c /= 10.0;
    c = 1.5 - sqrt(max(c, 0.0));
    float foam = clamp(c * c * c * c, 0.0, 1.0);
    float body = clamp(0.22 + foam * 0.82, 0.0, 1.0);
    vec3 base = vec3(0.0, 0.3, 0.5) + vec3(foam);
    vec3 color = base * mix(vec3(0.85, 0.96, 1.08), tint, 0.28);
    alphaEnergy = body;
    return clamp(color, 0.0, 1.0);
}

vec3 silkEffect(vec2 uv, float time, vec3 tint, out float alphaEnergy) {
    vec2 p = uv * 6.0;
    float flow = fbm2(p * 0.42 + vec2(time * 0.05, -time * 0.04));
    p += vec2(flow * 1.7, fbm2(p * 0.58 - time * 0.035) * 1.25);
    float ribbonA = pow(max(0.0, sin((p.x + p.y * 0.45) * 5.4 + time * 0.52 + flow * 3.2)), 3.2);
    float ribbonB = pow(max(0.0, sin((p.x * 0.38 - p.y) * 7.1 - time * 0.42 + flow * 2.8)), 4.5);
    float haze = smoothstep(0.18, 0.92, fbm2(p * 0.72 + flow));
    alphaEnergy = clamp(haze * 0.38 + ribbonA * 0.52 + ribbonB * 0.32, 0.0, 1.0);
    vec3 pearl = vec3(0.62, 0.86, 1.15);
    return clamp((tint * 0.58 + pearl * 0.42) * (0.34 + alphaEnergy * 1.28), 0.0, 1.0);
}

vec3 auroraEffect(vec2 uv, float time, vec3 tint, out float alphaEnergy) {
    vec2 p = uv * vec2(5.2, 7.2);
    float drift = fbm2(p * 0.38 + vec2(time * 0.04, -time * 0.03));
    float curtain = 0.0;

    for (int i = 0; i < 4; i++) {
        float fi = float(i);
        float wave = sin(p.x * (1.2 + fi * 0.34) + time * (0.34 + fi * 0.07) + drift * 4.0 + fi * 1.7);
        float band = exp(-abs(p.y * 0.72 + wave * 0.54 + fi * 0.26 - 0.15) * (2.4 + fi * 0.55));
        curtain += band * (0.58 - fi * 0.08);
    }

    float shimmer = pow(max(0.0, sin(p.x * 9.0 + drift * 7.0 + time * 1.3)), 4.0);
    alphaEnergy = clamp(curtain * (0.78 + shimmer * 0.25), 0.0, 1.0);
    vec3 teal = vec3(0.30, 1.20, 1.36);
    vec3 violet = vec3(0.74, 0.52, 1.35);
    return clamp(mix(teal, violet, smoothstep(0.2, 1.0, drift + shimmer * 0.22)) * (0.20 + alphaEnergy * 1.35) * mix(vec3(1.0), tint, 0.35), 0.0, 1.0);
}

vec3 nebulaEffect(vec2 uv, float time, vec3 tint, out float alphaEnergy) {
    vec2 p = uv * 4.4;
    p = rotate2(0.34) * p;
    float n1 = fbm2(p * 0.76 + vec2(time * 0.025, -time * 0.018));
    float n2 = fbm2(p * 1.38 - vec2(time * 0.018, time * 0.031) + n1 * 1.4);
    float smoke = smoothstep(0.28, 0.92, n1 * 0.78 + n2 * 0.56);
    float veins = pow(max(0.0, sin((p.x + p.y * 0.52 + n2 * 2.5) * 3.8 + time * 0.28)), 3.0);
    alphaEnergy = clamp(smoke * 0.62 + veins * smoke * 0.36, 0.0, 1.0);
    vec3 deep = vec3(0.18, 0.24, 0.46);
    vec3 bloom = vec3(0.72, 0.86, 1.24);
    vec3 color = mix(deep, bloom, smoke) + tint * veins * 0.28;
    return clamp(color * (0.34 + alphaEnergy * 0.95), 0.0, 1.0);
}

vec3 milkyVeilEffect(vec2 uv, float time, vec3 tint, out float alphaEnergy) {
    vec2 p = uv * 3.7;
    p = rotate2(-0.22) * p;
    float base = fbm2(p * 0.58 + vec2(time * 0.012, -time * 0.018));
    float detail = fbm2(p * 1.42 + base * 2.4 - vec2(time * 0.026, time * 0.015));
    float folds = pow(max(0.0, sin((p.x * 0.72 + p.y * 0.45 + detail * 3.3) * 4.0 + time * 0.20)), 2.2);
    float smoke = smoothstep(0.20, 0.86, base * 0.68 + detail * 0.74);
    float softVoid = smoothstep(0.18, 0.62, fbm2(p * 0.34 - base));
    alphaEnergy = clamp((smoke * 0.78 + folds * 0.28) * (0.58 + softVoid * 0.42), 0.0, 1.0);
    vec3 ash = vec3(0.34, 0.38, 0.52);
    vec3 milk = vec3(0.86, 0.89, 1.02);
    vec3 color = mix(ash, milk, smoke) + vec3(0.28, 0.32, 0.42) * folds;
    return clamp(mix(color, tint * 1.05, 0.12) * (0.40 + alphaEnergy * 0.92), 0.0, 1.0);
}


float seamlessNoise2(vec2 p, float width) {
    float x = mod(p.x, width);
    float t = smoothstep(0.0, 1.0, x / width);
    return mix(valueNoise2(vec2(x, p.y)), valueNoise2(vec2(x - width, p.y)), t);
}

float seamlessHash21(vec2 p, float width) {
    float x = mod(p.x, width);
    float t = smoothstep(0.0, 1.0, x / width);
    return mix(hash21(vec2(x, p.y)), hash21(vec2(x - width, p.y)), t);
}

vec3 skyStripesEffect(vec3 direction, float time, vec3 tint, out float alphaEnergy) {
    const float TAU = 6.28318530718;

    vec3 d = normalize(direction);

    float azimuth = fract(atan(d.z, d.x) / TAU + 0.5);
    float altitude = clamp(d.y, -1.0, 1.0);
    float altitude01 = clamp((altitude + 0.08) / 1.08, 0.0, 1.0);

    vec3 fwd = normalize(cameraForward.xyz);
    float viewAzimuth = fract(atan(fwd.z, fwd.x) / TAU + 0.5);
    float azToView = abs(fract(azimuth - viewAzimuth + 0.5) - 0.5);
    float viewGlow = exp(-(azToView * azToView) / (2.0 * 0.18 * 0.18));

    float verticalEnvelope = sin(clamp((altitude01 - 0.03) / 0.82, 0.0, 1.0) * 3.14159265);
    verticalEnvelope = pow(clamp(verticalEnvelope, 0.0, 1.0), 1.4);

    float edgeSharpening = mix(3.2, 1.8, verticalEnvelope);

    vec2 p = vec2(azimuth * 6.0, altitude01 * 1.8);
    float warp1 = seamlessNoise2(p + vec2(time * 0.04, time * 0.02), 6.0) - 0.5;
    float warp2 = seamlessNoise2(p * 2.0 - vec2(time * 0.03, time * 0.05), 12.0) - 0.5;
    vec2 warpedP = p + vec2(warp1 * 0.8 + warp2 * 0.3, warp1 * 0.4);

    float baseFill = seamlessNoise2(vec2(warpedP.x * 1.2 + time * 0.01, warpedP.y * 0.6), 7.2);
    baseFill = mix(0.05, 0.28, baseFill) * verticalEnvelope;

    float strandsNoise = seamlessNoise2(warpedP * 3.0, 18.0);
    float strands = sin(warpedP.x * 21.9911486 + strandsNoise * 4.0) * 0.5 + 0.5;
    strands = pow(strands, 1.8) * 0.12 * verticalEnvelope;

    float fold1Raw = abs(seamlessNoise2(vec2(warpedP.x * 2.8 + time * 0.02, warpedP.y * 0.8), 16.8) - 0.5) * 2.0;
    float fold1 = pow(1.0 - fold1Raw, edgeSharpening) * 0.42;

    float fold2Raw = abs(seamlessNoise2(vec2(warpedP.x * 6.0 - time * 0.04, warpedP.y * 1.8), 36.0) - 0.5) * 2.0;
    float fold2 = pow(1.0 - fold2Raw, edgeSharpening) * 0.22;

    float rawPattern = baseFill + strands + fold1 + fold2;

    float curtainPattern = (1.0 - exp(-rawPattern * 1.35)) * verticalEnvelope;

    float subtleGrain = seamlessHash21(vec2(azimuth * 350.0, altitude01 * 200.0 + time * 0.5), 350.0);
    float grainFilter = mix(0.94, 1.0, subtleGrain);

    curtainPattern *= grainFilter;
    curtainPattern *= mix(0.85, 1.15, viewGlow);

    vec3 darkTint = tint * 0.15;
    vec3 mainTint = tint * 0.85;
    vec3 brightTint = mix(tint, vec3(1.0), 0.28);

    vec3 curtainColor = mix(darkTint, mainTint, smoothstep(0.05, 0.45, curtainPattern));
    curtainColor = mix(curtainColor, brightTint, smoothstep(0.45, 0.85, curtainPattern));

    alphaEnergy = clamp(curtainPattern * 0.9, 0.0, 1.0);
    return clamp(curtainColor * curtainPattern, 0.0, 1.0);
}


vec3 celestialRiftEffect(vec3 direction, float time, vec3 tint) {
    vec3 normal = normalize(vec3(-0.48, 0.62, 0.62));
    vec3 tangent = normalize(cross(normal, vec3(0.0, 1.0, 0.0)));
    vec3 bitangent = cross(normal, tangent);
    vec2 along = vec2(dot(direction, tangent), dot(direction, bitangent));
    float across = dot(direction, normal);
    float t = time * 0.045;
    float warp = valueNoise2(along * 8.0 + vec2(t, -t * 0.3));
    float detail = valueNoise2(along * 27.0 - vec2(t * 0.4, t));
    float center = (warp - 0.5) * 0.12 + (detail - 0.5) * 0.025;
    float width = 0.018 + 0.035 * warp;
    float distanceToRift = abs(across - center);
    float edgeDistance = abs(distanceToRift - width);
    float pixel = max(fwidth(across - center), 0.0007);
    if (skyOccluded) return vec3(0.0);
    float interior = 1.0 - smoothstep(width - pixel, width + pixel, distanceToRift);
    float rim = exp(-edgeDistance / max(0.003, pixel));
    float glow = exp(-edgeDistance * 38.0) * 0.28;

    float cloud = fbm2(along * 12.0 + vec2(across * 18.0 + t, -t));
    vec3 nebula = mix(vec3(0.12, 0.06, 0.30), vec3(0.70, 0.20, 0.65), cloud);
    nebula = mix(nebula, vec3(0.12, 0.78, 0.95), smoothstep(0.50, 0.80, cloud));
    float travelingLight = 0.70 + 0.30 * sin(along.x * 18.0 + along.y * 12.0 - time * 0.5);
    vec3 light = nebula * interior * 0.85;
    light += vec3(0.40, 0.85, 1.0) * (rim * travelingLight + glow);

    for (int i = 0; i < 2; i++) {
        float side = i == 0 ? -1.0 : 1.0;
        float branch = center + side * (0.09 + 0.045 * sin(along.x * 9.0 + along.y * 5.0));
        float mask = smoothstep(0.45, 0.75, valueNoise2(along * 13.0 + float(i) * 7.0));
        float filament = exp(-abs(across - branch) / max(0.002, pixel));
        light += vec3(0.30, 0.55, 1.0) * filament * mask * 0.40;
    }

    float horizon = smoothstep(0.0, 0.14, direction.y);
    return (vec3(1.0) - exp(-light)) * horizon * mix(vec3(1.0), tint, 0.30);
}


vec3 meteorShowerEffect(vec3 direction, float time, vec3 tint) {
    vec3 light = vec3(0.0);
    float pixel = max(length(fwidth(direction)), 0.00035);
    if (skyOccluded) return vec3(0.0);

    for (int i = 0; i < 5; i++) {
        float lane = float(i);
        float period = 7.0 + lane * 1.73;
        float clock = time + lane * 3.91;
        float cycle = floor(clock / period);
        float age = mod(clock, period);
        float seed = hash21(vec2(cycle, lane + 0.37));
        float azimuth = seed * 6.28318530718;
        float elevation = 0.35 + hash21(vec2(lane + 5.0, cycle)) * 0.50;
        vec3 origin = vec3(cos(azimuth) * cos(elevation), sin(elevation), sin(azimuth) * cos(elevation));
        vec3 sideways = vec3(-sin(azimuth), 0.0, cos(azimuth));
        vec3 upward = cross(sideways, origin);
        vec3 travel = normalize(sideways * 0.70 - upward * 0.72);
        vec3 across = cross(origin, travel);
        float facing = dot(direction, origin);
        vec2 p = vec2(dot(direction, travel), dot(direction, across)) / max(facing, 0.20);
        float head = -0.22 + min(age, 1.2) * 0.48;
        float behind = head - p.x;
        float tailLength = 0.22;
        float segment = smoothstep(-pixel, pixel, behind)
                * (1.0 - smoothstep(tailLength * 0.65, tailLength, behind));
        float width = max(0.0008, pixel * 0.65);
        float trail = exp(-abs(p.y) / width) * segment
                * exp(-max(behind, 0.0) * 8.0);
        float core = exp(-length(vec2(p.x - head, p.y)) / (width * 1.8));
        float glow = exp(-length(vec2(p.x - head, p.y)) / (width * 5.0)) * 0.15;
        float life = smoothstep(0.0, 0.10, age) * (1.0 - smoothstep(0.85, 1.65, age));
        float brightness = (0.65 + seed * 0.60) * life * smoothstep(0.20, 0.40, facing);
        vec3 color = mix(vec3(0.46, 0.72, 1.0), vec3(1.0, 0.93, 0.78), seed);
        light += (color * trail + vec3(1.0, 0.97, 0.90) * core + color * glow) * brightness;
    }

    return light * smoothstep(0.01, 0.10, direction.y) * mix(vec3(1.0), tint, 0.20);
}

vec4 cosmicStormEffect(vec3 direction, float time, vec3 tint) {
    vec2 p = direction.xz / max(direction.y + 0.35, 0.35) * 2.0;
    float pixel = max(length(fwidth(p)), 0.002);
    if (skyOccluded) return vec4(0.0);
    vec2 drift = vec2(time * 0.016, -time * 0.009);
    float cloud = fbm2(p * 1.3 + drift);
    float folds = valueNoise2(p * 5.0 + cloud * 2.0 + drift);
    float density = smoothstep(0.25, 0.76, cloud * 0.80 + folds * 0.25);
    vec3 light = vec3(0.0);

    for (int i = 0; i < 3; i++) {
        float lane = float(i);
        float clock = time + lane * 2.73;
        float cycle = floor(clock / 8.7);
        float age = mod(clock, 8.7);
        float flash = smoothstep(0.0, 0.07, age) * exp(-age * 4.0);
        vec2 center = vec2(hash21(vec2(cycle, lane)), hash21(vec2(lane + 9.0, cycle))) * 5.0 - 2.5;
        vec2 q = p - center;
        float bend = (valueNoise2(vec2(q.y * 15.0, cycle + lane * 7.0)) - 0.5) * 0.28;
        float boltDistance = abs(q.x - bend - q.y * 0.18);
        float extent = 1.0 - smoothstep(0.35, 0.65, abs(q.y));
        float bolt = exp(-boltDistance / max(0.006, pixel * 0.65)) * extent;
        float branchDistance = abs(q.x - bend + q.y * 0.65 - 0.06);
        float branch = exp(-branchDistance / max(0.004, pixel * 0.5))
                * smoothstep(-0.05, 0.05, q.y) * (1.0 - smoothstep(0.18, 0.44, q.y));
        float illumination = exp(-dot(q, q) * 1.7) * (0.35 + density * 0.65);
        light += (vec3(0.68, 0.84, 1.0) * (bolt + branch * 0.50)
                + vec3(0.28, 0.40, 0.85) * illumination) * flash;
    }

    float horizon = smoothstep(0.01, 0.16, direction.y);
    float opacity = density * horizon * 0.72;
    vec3 clouds = mix(vec3(0.015, 0.022, 0.035), vec3(0.065, 0.085, 0.12), folds);
    return vec4(clouds * opacity + light * horizon * mix(vec3(1.0), tint, 0.35), opacity);
}




vec4 pearlEffect(vec3 direction, float time, vec3 tint) {
    vec2 p = vec2(dot(direction, normalize(vec3(0.8, 0.3, 0.5))),
            dot(direction, normalize(vec3(-0.3, 0.9, 0.2))));
    float t = time * 0.018;
    float folds = fbm2(p * 2.3 + vec2(t, -t * 0.5));
    float sheen = 0.5 + 0.5 * sin(p.x * 2.5 + p.y * 1.8 + folds * 4.0 + t);
    float pearl = smoothstep(0.35, 0.85, folds);
    vec3 rose = vec3(0.72, 0.48, 0.62);
    vec3 teal = vec3(0.36, 0.67, 0.70);
    vec3 color = mix(rose, teal, sheen);
    color = mix(color, vec3(0.88, 0.87, 0.80), pearl * 0.55);
    color = mix(color, tint, 0.22);
    float alpha = smoothstep(-0.06, 0.18, direction.y) * (0.18 + pearl * 0.24);
    return vec4(color * alpha, alpha);
}


float tideBoundary(float cycle) {
    return cycle * 64.0 + (hash21(vec2(cycle, 7.31)) - 0.5) * 24.0;
}

vec4 celestialTideEffect(vec3 direction, float time, vec3 tint) {
    float angularPixel = length(fwidth(direction.xz)) / max(length(direction.xz), 0.05);
    if (skyOccluded) return vec4(0.0);
    float elevation = asin(clamp(direction.y, -1.0, 1.0)) / 1.57079632679;
    float cycle = floor(time / 64.0);
    if (time < tideBoundary(cycle)) {
        cycle -= 1.0;
    } else if (time >= tideBoundary(cycle + 1.0)) {
        cycle += 1.0;
    }
    float start = tideBoundary(cycle);
    float phase = (time - start) / (tideBoundary(cycle + 1.0) - start);
    float seed = hash21(vec2(cycle, 19.73));
    float tide = smoothstep(0.10, 0.48, phase)
            * (1.0 - smoothstep(0.66, 0.96, phase));
    float height = mix(0.055, mix(0.38, 0.56, seed), tide);
    float horizontal = length(direction.xz);
    vec2 orbit = horizontal > 0.0001 ? direction.xz / horizontal : vec2(1.0, 0.0);
    vec2 flow = rotate2(time * 0.0018) * orbit;
    float broad = valueNoise2(flow * 3.0 + vec2(11.3, 7.8));
    float medium = valueNoise2(flow * 19.0 + vec2(29.7, 3.2));
    float fine = valueNoise2(flow * 62.0 + vec2(47.1, 15.0));
    fine = mix(fine, 0.5, smoothstep(0.4, 1.5, angularPixel * 62.0));

    float localHeight = height * (0.65 + broad * 0.45 + medium * 0.30);
    float altitude = max(elevation, 0.0);
    float normalizedHeight = altitude / max(localHeight, 0.01);
    float body = exp(-normalizedHeight * normalizedHeight * 2.8);
    float rayHeight = localHeight * (0.70 + medium * 0.55);
    float rays = exp(-altitude / max(rayHeight, 0.01) * 3.0)
            * (0.12 + medium * medium * 0.75 + fine * fine * 0.35);
    float horizonGlow = exp(-altitude / (0.026 + tide * 0.035));
    float visibility = smoothstep(-0.055, 0.012, elevation)
            * (1.0 - smoothstep(0.70, 0.90, elevation));
    float strength = 0.35 + tide * 0.65;

    vec3 violet = vec3(0.48, 0.08, 0.82);
    vec3 blue = vec3(0.06, 0.22, 0.78);
    vec3 horizonBlue = vec3(0.16, 0.34, 0.88);
    vec3 color = mix(violet, blue, smoothstep(0.15, 1.0, normalizedHeight));
    vec3 light = color * body * 0.55;
    light += mix(violet, horizonBlue, medium * 0.45) * rays * 0.60;
    light += horizonBlue * horizonGlow * (0.25 + broad * 0.15);
    light /= 1.0 + max(light.r, max(light.g, light.b)) * 0.45;
    light *= strength * visibility * mix(vec3(1.0), tint, 0.12);

    float alpha = clamp(body * strength * visibility * 0.18, 0.0, 0.18);
    return vec4(light, alpha);
}

vec3 planeEffect(float mode, vec2 uv, float time, vec3 tint, out float alphaEnergy) {
    if (mode < 0.5) {
        return waterEffect(uv, time * 0.125, tint, alphaEnergy);
    }
    if (mode < 1.5) {
        return silkEffect(uv, time, tint, alphaEnergy);
    }
    if (mode < 2.5) {
        return auroraEffect(uv, time, tint, alphaEnergy);
    }
    if (mode < 3.5) {
        return nebulaEffect(uv, time, tint, alphaEnergy);
    }
    return milkyVeilEffect(uv, time, tint, alphaEnergy);
}

vec3 seamlessSkyEffect(vec3 direction, float time, vec3 tint, float mode, out float alphaEnergy) {
    vec3 d = normalize(direction);
    vec3 weights = pow(max(abs(d), vec3(0.001)), vec3(3.0));
    weights /= max(weights.x + weights.y + weights.z, 0.001);

    float energyX = 0.0;
    float energyY = 0.0;
    float energyZ = 0.0;
    vec3 effectX = planeEffect(mode, d.yz * 0.5 + vec2(0.17, -0.31), time, tint, energyX);
    vec3 effectY = planeEffect(mode, d.xz * 0.5 + vec2(-0.43, 0.09), time, tint, energyY);
    vec3 effectZ = planeEffect(mode, d.xy * 0.5 + vec2(0.28, 0.37), time, tint, energyZ);

    alphaEnergy = dot(vec3(energyX, energyY, energyZ), weights);
    return effectX * weights.x + effectY * weights.y + effectZ * weights.z;
}

void main() {
    skyOccluded = skyHidden(texCoord);
    float mode = floor(meta.w + 0.5);
    if (skyOccluded && (mode < 5.5 || (mode > 8.5 && mode < 9.5))) {
        discard;
    }

    vec3 direction = skyDirection(texCoord);
    vec2 verticalUv = vec2(texCoord.x, clamp(direction.y * 0.5 + 0.5, 0.0, 1.0));
    float time = meta.z;
    float intensity = params.y;
    float fade = verticalFade(verticalUv);
    vec3 tint = clamp(tintColor(verticalUv), 0.0, 1.0);
    float energy = 0.0;


    if (mode > 8.5) {
        float strength = clamp(skyColor.a * intensity, 0.0, 1.0);
        if (mode < 9.5) {
            fragColor = pearlEffect(direction, time, tint) * strength;
        } else {
            fragColor = celestialTideEffect(direction, time, tint) * strength;
        }
        return;
    }

    if (mode > 7.5 && mode < 8.5) {
        vec4 storm = cosmicStormEffect(direction, time, tint);
        float strength = clamp(skyColor.a * intensity, 0.0, 1.0);
        fragColor = storm * strength;
        return;
    }

    if (mode > 5.5) {
        vec3 light;
        if (mode < 6.5) {
            light = celestialRiftEffect(direction, time, tint);
        } else {
            light = meteorShowerEffect(direction, time, tint);
        }
        fragColor = vec4(light * skyColor.a * intensity, 0.0);
        return;
    }

    if (mode > 4.5) {
        vec3 stripes = skyStripesEffect(direction, time, tint, energy);
        float stripesAlpha = clamp(energy * skyColor.a * intensity, 0.0, 0.995);
        fragColor = vec4(stripes * stripesAlpha, 0.0);
        return;
    }

    vec3 effect = seamlessSkyEffect(direction, time, tint, mode, energy);

    float alpha = clamp((0.12 + energy * 0.48) * skyColor.a * intensity * fade, 0.0, 0.74);
    fragColor = vec4(clamp(effect * intensity, 0.0, 1.0) * alpha, alpha);
}

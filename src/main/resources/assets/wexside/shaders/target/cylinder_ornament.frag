#version 330
in vec2 uv;
out vec4 fragColor;
layout(std140) uniform CylinderData { mat4 mvp; vec4 meta; vec4 tint; vec4 shape; };
float hash(vec2 p) { p = fract(p * vec2(123.34, 345.45)); p += dot(p, p + 34.345); return fract(p.x * p.y); }
float segment(vec2 p, vec2 a, vec2 b) { vec2 d = b - a; return length(p - a - d * clamp(dot(p - a, d) / max(dot(d, d), 0.0001), 0.0, 1.0)); }
float stroke(float d, float width) { return 1.0 - smoothstep(width, width + max(fwidth(d), 0.006), d); }
float electricStroke(float d, float width) {
    float aa = max(fwidth(d), 0.0015);
    return 1.0 - smoothstep(width, width + aa, d);
}
float hoopHeight(float x, float time) {
    return 0.55 + sin(x * 1.04719755 + time * 0.32) * 0.16;
}
vec2 dischargePoint(float index, float seed, float width, float center, float time) {
    float t = index / 12.0;
    float irregular = t + (hash(vec2(index, seed + 5.0)) - 0.5) * 0.055 * sin(t * 3.14159265);
    float x = mix(-width, width, irregular);
    float coarse = t * 3.0;
    float cell = floor(coarse);
    float bend = mix(hash(vec2(cell, seed)), hash(vec2(cell + 1.0, seed)), fract(coarse)) - 0.5;
    float detail = (hash(vec2(index + 23.0, seed)) - 0.5) * 0.13;
    float displacement = (bend * 0.95 + detail) * sin(t * 3.14159265);
    float baseline = hoopHeight(center + x, time);
    float room = displacement > 0.0 ? 0.94 - baseline : baseline - 0.06;
    float y = baseline + sign(displacement) * room * tanh(abs(displacement) / room);
    return vec2(x, y);
}
vec3 lightning(float time) {
    float x = uv.x * 6.0;
    float ringDistance = abs(uv.y - hoopHeight(x, time));
    float intact = 1.0, core = 0.0, glow = 0.0;
    for (int gap = 0; gap < 3; gap++) {
        float id = float(gap);
        float center = time * 0.28 + id * 2.0 + sin(time * 0.43 + id * 2.4) * 0.28;
        float width = 0.6 + sin(time * 0.38 + id * 1.7) * 0.1;
        vec2 q = vec2(mod(x - center + 3.0, 6.0) - 3.0, uv.y);
        float cut = smoothstep(width - 0.014, width + 0.014, abs(q.x));
        intact *= cut;
        float clock = time * (1.3 + id * 0.14) + id * 0.37;
        float age = fract(clock);
        float seed = hash(vec2(floor(clock), id + 41.0)) * 97.0;
        float nextSeed = hash(vec2(floor(clock) + 1.0, id + 41.0)) * 97.0;
        float reshape = smoothstep(0.88, 1.0, age);
        float onset = 0.06 + hash(vec2(seed, 7.0)) * 0.2;
        float duration = 0.14 + hash(vec2(seed, 13.0)) * 0.16;
        float strength = 0.65 + hash(vec2(seed, 19.0)) * 0.65;
        float primary = smoothstep(onset, onset + 0.025, age)
            * (1.0 - smoothstep(onset + duration, onset + duration + 0.12, age));
        float echoStart = 0.62 + hash(vec2(seed, 29.0)) * 0.12;
        float echo = smoothstep(echoStart, echoStart + 0.018, age)
            * (1.0 - smoothstep(echoStart + 0.05, echoStart + 0.13, age));
        echo *= step(0.58, hash(vec2(seed, 31.0)));
        float flash = max(primary, echo * 0.85) * strength;
        float d = 10.0, branchLight = 0.0, movingCharge = 0.0;
        vec2 a = dischargePoint(0.0, seed, width, center, time);
        for (int part = 1; part <= 12; part++) {
            float index = float(part);
            vec2 b = dischargePoint(index, seed, width, center, time);
            b = mix(b, dischargePoint(index, nextSeed, width, center, time), reshape);
            float distanceToSegment = segment(q, a, b);
            d = min(d, distanceToSegment);
            vec2 direction = b - a;
            float along = clamp(dot(q - a, direction) / max(dot(direction, direction), 0.00001), 0.0, 1.0);
            float position = (index - 1.0 + along) / 12.0;
            float front = smoothstep(onset, onset + duration, age);
            float echoFront = smoothstep(echoStart, echoStart + 0.075, age);
            if (hash(vec2(seed, 37.0)) < 0.5) {
                front = 1.0 - front;
            } else {
                echoFront = 1.0 - echoFront;
            }
            float packet = max(exp(-pow((position - front) * 7.0, 2.0)) * primary,
                exp(-pow((position - echoFront) * 9.0, 2.0)) * echo) * strength;
            movingCharge = max(movingCharge, electricStroke(distanceToSegment, 0.018) * packet);
            if (part == 3 || part == 6 || part == 9) {
                float random = mix(hash(vec2(seed, index)), hash(vec2(nextSeed, index)), reshape);
                float side = random < 0.5 ? -1.0 : 1.0;
                if (b.y > 0.62) side = -1.0;
                if (b.y < 0.38) side = 1.0;
                float reach = min(0.18 + random * 0.22, side > 0.0 ? 0.94 - b.y : b.y - 0.06);
                vec2 c = b + vec2(width * 0.22, side * reach * 0.42);
                vec2 e = c + vec2(-width * (0.07 + random * 0.22), side * reach * 0.27);
                vec2 f = e + vec2(width * 0.34, side * reach * 0.31);
                float branch = electricStroke(segment(q, b, c), 0.006);
                branch = max(branch, electricStroke(segment(q, c, e), 0.004) * 0.85);
                branch = max(branch, electricStroke(segment(q, e, f), 0.0022) * 0.6);
                branchLight += branch * (0.65 + random * 0.3);
            }
            a = b;
        }
        core += electricStroke(d, 0.009) * (0.48 + flash * 0.25);
        core += branchLight * (0.22 + flash * 0.5) + movingCharge * 0.6;
        glow += exp(-d * 42.0) * (0.16 + flash * 0.17);
    }
    float ringCore = electricStroke(ringDistance, 0.014) * intact;
    float ringGlow = exp(-ringDistance * 42.0) * intact;
    float trailHeight = hoopHeight(x, time);
    float trail = smoothstep(0.0, 1.0, uv.y / trailHeight);
    trail *= 1.0 - smoothstep(trailHeight - 0.008, trailHeight + max(fwidth(uv.y), 0.003), uv.y);
    float edge = smoothstep(0.0, 0.07, uv.y) * (1.0 - smoothstep(0.93, 1.0, uv.y));
    vec3 hot = mix(tint.rgb, vec3(1.0), 0.32);
    return (hot * (core * (1.1 + meta.z * 0.4) + ringCore * 0.58)
        + tint.rgb * (glow + ringGlow * 0.18 + trail * 0.14)) * edge;
}
void main() {
    float time = meta.x;
    int mode = int(meta.y + 0.5);
    if (mode == 6) {
        fragColor = vec4(lightning(time) * meta.w, 0.0);
        return;
    }
    float pattern = 0.0;
    float ring = 1.0 - smoothstep(0.012, 0.012 + max(fwidth(uv.y) * 1.5, 0.008), 1.0 - uv.y);
    if (mode == 2) {
        vec2 p = vec2(uv.x * 20.0 + time * 0.32, uv.y);
        vec2 cell = floor(p);
        vec2 q = fract(p);
        float seed = hash(vec2(mod(cell.x, 20.0), cell.y));
        float age = fract(time * 0.25 + seed);
        float center = mix(0.92, 0.12, smoothstep(0.0, 1.0, age));
        float size = 0.36 + sin(age * 3.14159265) * 0.2;
        q.y = (uv.y - center) / size + 0.5;
        q.x += sin(age * 3.14159265) * sin(seed * 12.0) * 0.11;
        float life = smoothstep(0.0, 0.14, age) * (1.0 - smoothstep(0.65, 1.0, age));
        float angle = sin(age * 3.14159265) * sin(seed * 6.28) * 0.45;
        q = mat2(cos(angle), -sin(angle), sin(angle), cos(angle)) * (q - 0.5) + 0.5;
        float d = segment(q, vec2(0.18, 0.82), vec2(0.32, 0.18));
        d = min(d, segment(q, vec2(0.32, 0.18), vec2(0.5, 0.62)));
        d = min(d, segment(q, vec2(0.5, 0.62), vec2(0.68, 0.18)));
        d = min(d, segment(q, vec2(0.68, 0.18), vec2(0.82, 0.82)));
        pattern = stroke(d, 0.033) * life;
        pattern *= smoothstep(age * 0.5, age * 0.5 + 0.15, hash(floor(q * 7.0) + cell));
    }
    float fade = smoothstep(0.0, 0.22, uv.y);
    vec3 color = tint.rgb;
    vec3 bright = mix(color, vec3(1.0), 0.18);
    vec3 light = color * fade * 0.025 + bright * ring * 0.7;
    light += color * pattern * fade * (0.6 + meta.z * 0.25);
    fragColor = vec4(light * meta.w, 0.0);
}

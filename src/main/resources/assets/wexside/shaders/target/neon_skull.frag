#version 330 core
layout(std140) uniform SkullData { mat4 mvp; vec4 tint; vec4 animation; vec4 ending; };
in vec2 point;
out vec4 fragColor;

float box(vec2 p, vec2 r, float rounding) {
    vec2 q = abs(p) - r + rounding;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rounding;
}
float segment(vec2 p, vec2 a, vec2 b) {
    vec2 v = b - a;
    return length(p - a - v * clamp(dot(p - a, v) / dot(v, v), 0.0, 1.0));
}
float crackX(float y) {
    if (y > 0.64) return mix(0.05, -0.12, clamp((y - 0.64) / 0.27, 0.0, 1.0));
    if (y > 0.45) return mix(-0.07, 0.05, (y - 0.45) / 0.19);
    if (y > 0.29) return mix(0.12, -0.07, (y - 0.29) / 0.16);
    return mix(0.015, 0.12, clamp((y - 0.04) / 0.25, 0.0, 1.0));
}
float eyeWisp(vec2 p, float age, float side) {
    float along = (p.y - 0.09) / 1.02;
    float height = clamp(along, 0.0, 1.0);
    float head = age * 1.9;
    float tail = max(0.0, (age - 0.24) * 1.65);
    float envelope = smoothstep(tail - 0.12, tail + 0.10, along)
                   * (1.0 - smoothstep(head - 0.18, head, along));
    envelope *= 1.0 - smoothstep(0.78, 1.0, along);
    float bend = 0.43 * smoothstep(0.0, 0.62, height)
               - 0.20 * smoothstep(0.60, 1.0, height);
    float curl = sin(height * 8.0 - age * 3.0 + side * 0.35) * 0.045 * sin(height * 3.14159265);
    float center = side * (0.345 + bend + curl);
    float width = (0.032 + 0.034 * sin(height * 3.14159265)) * (1.0 - height * 0.45);
    float across = (p.x - center) / width;
    float sheet = exp(-across * across * 1.4);
    float fold = exp(-pow((across - 0.32 * sin(height * 12.0 - age * 4.0)) * 3.0, 2.0));
    float haze = exp(-across * across * 0.24);
    return (sheet * 0.32 + fold * 0.28 + haze * 0.09) * envelope;
}
float smokeHash(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}
float smokeNoise(vec2 p) {
    vec2 cell = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(smokeHash(cell), smokeHash(cell + vec2(1,0)), f.x),
               mix(smokeHash(cell + vec2(0,1)), smokeHash(cell + vec2(1,1)), f.x), f.y);
}
float mouthStream(vec2 p, vec2 origin, vec2 direction, float lengthScale, float age, float seed) {
    vec2 relative = p - origin;
    float along = dot(relative, direction) / lengthScale;
    float across = dot(relative, vec2(-direction.y, direction.x));
    float spread = clamp(along, 0.0, 1.0);
    float head = age * 1.95;
    float tail = max(0.0, (age - 0.48) * 1.9);
    float envelope = smoothstep(tail - 0.14, tail + 0.06, along)
                   * (1.0 - smoothstep(head - 0.23, head + 0.06, along))
                   * (1.0 - smoothstep(0.65, 1.0, along));
    float bend = sin(spread * 6.0 - age * 2.5 + seed) * spread * 0.07;
    float width = mix(0.060, 0.17, spread);
    vec2 flow = vec2(along * 4.0 - age * 3.0, across * 11.0 + seed);
    float coarse = smokeNoise(flow);
    float detail = smokeNoise(flow * 2.1 + vec2(coarse * 1.7, seed));
    float warped = (across - bend + (coarse - 0.5) * width * 1.2) / width;
    float sheet = exp(-warped * warped * 1.6);
    float folds = smoothstep(0.18, 0.8, coarse * 0.65 + detail * 0.35);
    float haze = exp(-warped * warped * 0.5) * 0.065;
    return (sheet * (0.16 + folds * 0.55) + haze) * envelope;
}
float mouthBreath(vec2 p, float age, float jawDrop) {
    float mouthY = -0.635 - jawDrop * 0.5;
    float left = mouthStream(p, vec2(-0.20, mouthY), vec2(-0.96,-0.28), 0.72, age, 1.7);
    float right = mouthStream(p, vec2(0.20, mouthY), vec2(0.96,-0.28), 0.72, age * 0.97, 4.2);
    float down = mouthStream(p, vec2(0.0, mouthY), vec2(0.0,-1.0), 0.53, age * 0.9, 7.6);
    float source = exp(-pow(p.x / 0.24, 2.0) - pow((p.y - mouthY) / 0.075, 2.0))
                 * (1.0 - smoothstep(0.40, 0.80, age)) * 0.18;
    return left + right + down * 0.8 + source;
}
void curve(vec2 p, vec2 a, vec2 control, vec2 b, inout float distanceToEdge, inout bool inside) {
    vec2 previous = a;
    for (int i = 1; i <= 8; ++i) {
        float t = float(i) / 8.0;
        vec2 current = mix(mix(a, control, t), mix(control, b, t), t);
        distanceToEdge = min(distanceToEdge, segment(p, previous, current));
        if ((previous.y > p.y) != (current.y > p.y)) {
            float x = mix(previous.x, current.x, (p.y - previous.y) / (current.y - previous.y));
            if (p.x < x) inside = !inside;
        }
        previous = current;
    }
}
vec2 referencePoint(vec2 pixel) { return (vec2(pixel.x, -pixel.y) + vec2(-313.0, 418.0)) / 400.0; }
void outline(vec2 p, vec2 a, vec2 control, vec2 b, inout float d, inout bool inside) {
    curve(p, referencePoint(a), referencePoint(control), referencePoint(b), d, inside);
}
float skull(vec2 p, float jawDrop) {
    vec2 q = vec2(abs(p.x), p.y);
    float face = 10.0;
    bool inside = false;
    outline(q, vec2(313,28), vec2(480,28), vec2(558,142), face, inside);
    outline(q, vec2(558,142), vec2(599,199), vec2(598,274), face, inside);
    outline(q, vec2(598,274), vec2(599,393), vec2(571,450), face, inside);
    outline(q, vec2(571,450), vec2(594,506), vec2(562,548), face, inside);
    outline(q, vec2(562,548), vec2(540,578), vec2(510,568), face, inside);
    outline(q, vec2(510,568), vec2(483,563), vec2(463,563), face, inside);
    outline(q, vec2(463,563), vec2(434,563), vec2(427,608), face, inside);
    outline(q, vec2(427,608), vec2(423,652), vec2(418,662), face, inside);
    outline(q, vec2(418,662), vec2(365,662), vec2(313,662), face, inside);
    face *= inside ? -1.0 : 1.0;
    vec2 eyePoint = vec2(abs(p.x) - 0.345, p.y - 0.09);
    float eye = (pow(pow(abs(eyePoint.x / 0.221), 2.25) + pow(abs(eyePoint.y / 0.220), 2.25), 1.0 / 2.25) - 1.0) * 0.220;
    float nose = 10.0;
    inside = false;
    outline(q, vec2(313,422), vec2(348,422), vec2(369,528), nose, inside);
    outline(q, vec2(369,528), vec2(375,571), vec2(337,548), nose, inside);
    outline(q, vec2(337,548), vec2(319,541), vec2(313,537), nose, inside);
    nose *= inside ? -1.0 : 1.0;
    face = max(face, -min(eye, nose));
    vec2 jawPoint = vec2(q.x, q.y + jawDrop);
    float jaw = 10.0;
    inside = false;
    outline(jawPoint, vec2(313,808), vec2(420,808), vec2(508,732), jaw, inside);
    outline(jawPoint, vec2(508,732), vec2(508,664), vec2(508,598), jaw, inside);
    outline(jawPoint, vec2(508,598), vec2(484,592), vec2(463,591), jaw, inside);
    outline(jawPoint, vec2(463,591), vec2(456,615), vec2(452,651), jaw, inside);
    outline(jawPoint, vec2(452,651), vec2(448,690), vec2(418,690), jaw, inside);
    outline(jawPoint, vec2(418,690), vec2(365,690), vec2(313,690), jaw, inside);
    jaw *= inside ? -1.0 : 1.0;
    float teeth = 10.0;
    float jawTeeth = 10.0;
    for (int i = 0; i < 4; ++i) {
        float x = (-70.5 + float(i) * 47.0) / 400.0;
        teeth = min(teeth, box(p - vec2(x, -0.64), vec2(0.035,0.119), 0.034));
        jawTeeth = min(jawTeeth, box(p + vec2(0.0, jawDrop) - vec2(x, -0.64), vec2(0.035,0.119), 0.034));
    }
    return min(max(face, -teeth), max(jaw, -jawTeeth));
}
void main() {
    float time = animation.x;
    float stage = animation.y;
    float hit = animation.z;
    float death = ending.x;
    vec2 p = point;
    float jawDrop = ending.y + 0.14 * smoothstep(0.0, 0.85, death);
    float body = skull(p, jawDrop);
    float reach = mix(1.0, 0.15, smoothstep(0.0, 1.0, stage));
    float opening = mix(0.012, 0.052, smoothstep(1.0, 2.0, stage));
    float cut = max(abs(p.x - crackX(p.y)) - opening, reach - p.y);
    float damaged = max(body, -cut);
    float d = mix(body, damaged, smoothstep(0.0, 0.10, stage));
    if (death > 0.0) {
        float separation = smoothstep(0.0, 1.0, death);
        float splitDistance = 10.0;
        for (int i = 0; i < 2; ++i) {
            float side = i == 0 ? -1.0 : 1.0;
            float angle = side * separation * (i == 0 ? 0.16 : 0.23);
            vec2 pivot = vec2(side * 0.30, 0.20);
            vec2 local = point - pivot - vec2(side * separation * 0.13, -separation * (i == 0 ? 0.07 : 0.11));
            float c = cos(angle), s = sin(angle);
            local = vec2(c * local.x - s * local.y, s * local.x + c * local.y) + pivot;
            float halfPlane = -side * (local.x - crackX(local.y));
            float halfSkull = max(skull(local, jawDrop), halfPlane);
            splitDistance = min(splitDistance, halfSkull);
        }
        d = mix(d, splitDistance, smoothstep(0.0, 0.12, death));
    }
    float aa = max(fwidth(d), 0.0015);
    float edge = 1.0 - smoothstep(0.006, 0.006 + aa, abs(d));
    float halo = exp(-abs(d) * 35.0) * 0.45 + exp(-abs(d) * 12.0) * 0.12;
    float fill = (1.0 - smoothstep(-aa, aa, d)) * 0.055;
    float crackLight = exp(-abs(cut) * 45.0) * (1.0 - smoothstep(0.0, 0.06, body));
    float beat = pow(max(0.0, sin(time)), 24.0) * smoothstep(1.1, 1.9, stage);
    float travel = exp(-pow((p.y - mix(0.95, 0.15, fract(time / 6.2831853))) * 9.0, 2.0));
    crackLight *= smoothstep(0.0, 0.2, stage) * (0.55 + hit * 0.3 + beat * travel * 0.65);
    crackLight *= 1.0 - smoothstep(0.0, 0.12, death);
    float pulse = 0.96 + 0.04 * sin(time);
    float light = edge + halo + crackLight * 0.5;
    float hitTime = ending.z;
    float hitEnvelope = smoothstep(0.0, 0.06, hitTime) * (1.0 - smoothstep(0.65, 1.0, hitTime));
    hitEnvelope *= 1.0 - smoothstep(0.0, 0.15, death);
    float breath = 0.0;
    if (hitEnvelope > 0.001) {
        breath = (eyeWisp(p, hitTime, -1.0) + eyeWisp(p, hitTime * 0.95, 1.0)) * hitEnvelope;
        breath += mouthBreath(p, max(0.0, hitTime - 0.04), jawDrop) * hitEnvelope;
    }
    light += breath;
    float alpha = clamp((light + fill) * animation.w * tint.a * pulse, 0.0, 1.0);
    if (alpha < 0.002) discard;
    vec3 color = mix(tint.rgb, vec3(0.92,0.98,1.0), clamp(edge * 0.4 + crackLight * 0.3 + breath * 0.2, 0.0, 0.8));
    fragColor = vec4(color, alpha);
}

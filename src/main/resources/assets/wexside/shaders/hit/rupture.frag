#version 330
in vec4 tint;
in vec3 surfacePos;
in vec2 animation;
out vec4 fragColor;

void main() {
    float age = clamp(animation.x, 0.0, 1.0);
    float phase = animation.y * 6.2831853;
    vec3 p = surfacePos;
    float life = smoothstep(0.0, 0.06, age) * (1.0 - smoothstep(0.50, 1.0, age));
    float bend = sin(p.x * 4.2 + p.z * 3.1 + phase) * 0.12
               + sin(p.z * 6.0 - p.x * 2.4 + phase * 0.7) * 0.055;
    float flow = p.y + bend + p.x * 0.18;
    float travel = mix(-0.28, 1.80, smoothstep(0.0, 1.0, age));
    float distanceToWave = flow - travel;
    float aa = max(fwidth(flow), 0.002);
    float wave = 1.0 - smoothstep(0.045, 0.16 + aa, abs(distanceToWave));
    float wake = exp(-abs(distanceToWave + 0.16) * 5.5);
    float folds = sin(flow * 19.0 + sin(p.x * 5.0 + p.z * 4.0 + phase) * 1.3 - age * 3.0);
    float filament = smoothstep(0.72 - aa * 8.0, 0.98 + aa * 8.0, folds);
    float energy = wave * 0.78 + wake * (0.10 + filament * 0.32);
    float alpha = clamp(energy * life * tint.a, 0.0, 0.72);
    vec3 color = tint.rgb * (0.65 + wave * 0.48 + filament * 0.12);
    fragColor = vec4(clamp(color, 0.0, 1.0), alpha);
    if (alpha < 0.003) discard;
}

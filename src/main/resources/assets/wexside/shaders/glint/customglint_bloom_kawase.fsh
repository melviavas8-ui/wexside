#version 330

uniform sampler2D InSampler;

in vec2 texCoord;
out vec4 fragColor;

vec3 sample_bloom(vec2 uv) {
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        return vec3(0.0);
    }
    return texture(InSampler, uv).rgb;
}

void main() {
    vec2 texelSize = 1.0 / vec2(textureSize(InSampler, 0));
    float offset = float(BLOOM_KAWASE_OFFSET);
    vec2 diagonal = texelSize * offset;
    vec3 center = sample_bloom(texCoord) * 0.40;
    vec3 corners = vec3(0.0);
    corners += sample_bloom(texCoord + vec2(-diagonal.x, -diagonal.y));
    corners += sample_bloom(texCoord + vec2(diagonal.x, -diagonal.y));
    corners += sample_bloom(texCoord + vec2(-diagonal.x, diagonal.y));
    corners += sample_bloom(texCoord + vec2(diagonal.x, diagonal.y));
#if BLOOM_KAWASE_UPSAMPLE == 1
    vec3 edges = vec3(0.0);
    edges += sample_bloom(texCoord + vec2(-diagonal.x, 0.0));
    edges += sample_bloom(texCoord + vec2(diagonal.x, 0.0));
    edges += sample_bloom(texCoord + vec2(0.0, -diagonal.y));
    edges += sample_bloom(texCoord + vec2(0.0, diagonal.y));
    fragColor = vec4(center + corners * 0.105 + edges * 0.045, 1.0);
#else
    fragColor = vec4(center + corners * 0.15, 1.0);
#endif
}

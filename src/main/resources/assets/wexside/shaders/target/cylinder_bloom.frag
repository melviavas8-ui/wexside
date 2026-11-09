#version 330
in vec2 uv;
out vec4 fragColor;
uniform sampler2D SourceSampler;
uniform sampler2D BloomSampler;
void main() { fragColor = vec4(texture(SourceSampler, uv).rgb + texture(BloomSampler, uv).rgb * 0.65, 0.0); }

#version 330 core
layout(location = 0) in vec3 Position;
layout(location = 1) in vec3 Trail;
layout(location = 2) in vec2 AgePulse;
uniform mat4 uTransform;
out vec3 vTrail;
out vec2 vAgePulse;
void main() {
    gl_Position = uTransform * vec4(Position, 1.0);
    vTrail = Trail;
    vAgePulse = AgePulse;
}

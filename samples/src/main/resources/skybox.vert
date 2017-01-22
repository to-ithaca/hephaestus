#version 400
#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable
layout (std140, binding = 0) uniform buf {
        float scale;
        float theta;
} ubuf;
layout (location = 0) in vec2 pos;
layout (location = 0) out vec2 texcoord;
out gl_PerVertex { 
    vec4 gl_Position;
};
void main() {
   texcoord.y = pos.y * 0.5 + 0.5;
   texcoord.x = (pos.x * 0.5 + 0.5) * ubuf.scale + ubuf.theta;
   //texcoord = pos * 0.5 + 0.5;
   gl_Position = vec4(pos, 0.999999, 1.0);
}
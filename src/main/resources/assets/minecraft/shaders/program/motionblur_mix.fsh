#version 120

// PolyBlur "Phosphor Blur" port: linear mix trail (differs from vanilla
// phosphor.fsh, which is a max() glow trail).
// Source: PolyBlur 1.0.2 phosphor_motion_blur.fsh (Polyfrost, GPL-3.0 via repo license header).

uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;

varying vec2 texCoord;

uniform float Weight = 1.0;

void main() {
    vec4 CurrTexel = texture2D(DiffuseSampler, texCoord);
    vec4 PrevTexel = texture2D(PrevSampler, texCoord);

    gl_FragColor = vec4(mix(PrevTexel.rgb, CurrTexel.rgb, Weight), 1.0);
}

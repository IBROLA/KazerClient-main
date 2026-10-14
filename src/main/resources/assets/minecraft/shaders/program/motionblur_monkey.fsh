#version 120

// Depth-reprojection motion blur.
// Ported from MonkeyBlur, (c) 2021 DJtheRedstoner, MIT License
// (see assets/minecraft/shaders/LICENSE-MonkeyBlur.txt).
// CheatBreakerZ adaptations: samplers renamed (ColorTex/DepthTex), the unused
// maxDepth()/textureName lookup dropped, texCoord shared with sobel.vsh.

uniform sampler2D ColorTex;
uniform sampler2D DepthTex;

uniform mat4 modelViewInverse;
uniform mat4 projectionInverse;
uniform mat4 previousProjection;
uniform mat4 previousModelView;

uniform vec3 cameraPosition;
uniform vec3 previousCameraPosition;

uniform float strength;

varying vec2 texCoord;

void main() {
    vec3 color = texture2D(ColorTex, texCoord).rgb;
    float depth = texture2D(DepthTex, texCoord).x;

    vec4 hPos = vec4(texCoord, depth, 1.0) * 2.0 - 1.0;

    vec4 worldPos = projectionInverse * hPos;
    worldPos = modelViewInverse * worldPos;
    worldPos /= worldPos.w;
    worldPos.xyz += cameraPosition;

    vec4 previousPos = worldPos;
    previousPos.xyz -= previousCameraPosition;
    previousPos = previousModelView * previousPos;
    previousPos = previousProjection * previousPos;
    previousPos /= previousPos.w;

    vec2 velocity = (hPos - previousPos).st;
    velocity = velocity / (1.0 + length(velocity)) * strength * 0.01;

    vec2 coord = texCoord + velocity;
    float count = 1.0;
    for (int i = 0; i < 30; ++i, coord += velocity) {
        if (coord.s > 1.0 || coord.t > 1.0 || coord.s < 0.0 || coord.t < 0.0) {
            break;
        }
        color += texture2D(ColorTex, coord).rgb;
        count += 1.0;
    }
    color /= count;

    gl_FragColor = vec4(color, 1.0);
}

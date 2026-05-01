#version 450

layout(binding = 1) uniform UBO {
    vec4 ColorModulator;
    vec3 LightDir;
    vec3 LightColor;
    vec4 SkyColor;
    vec4 FogColor;
    vec3 UpVector;
    float FogStart;
    float FogEnd;
    float NightMultiplier;
    float LightVisibility;
};

layout(location = 0) in float vertexDistance;
layout(location = 1) in vec3 fragPos;

layout(location = 0) out vec4 fragColor;

vec4 getSkyColor(const float RdotL, float RoUp, const float SoUp) {
    //TODO precompute and optimize
    vec4 color;

    float f1 = 1.0 - 0.1 / (5.0 * pow(RoUp, 2.0) + 0.1);
    //    float f1 = smoothstep(0.0, 1.0, rDotUp * 2);
    //    float f1 = 1.0 - 1.0 / (pow(rDotUp, 2.0) * 15.0 + 1.0);
    color = mix(FogColor, SkyColor, f1);
//    color = SkyColor;

//    float sunDotUp = max(dot(UpVector, LightDir), 0.0);
    float m1 = (1 - SoUp);

    float m2 = pow(RdotL, 2.0);
    m2 *= pow(1.0 - RoUp, 4.0);
    m2 *= m1;
//    FdotL = pow(FdotL, 4.0);
//    FdotL *= pow(1.0 - RoUp, 8.0);
    //    fogColor = fogColor * (1 - FdotL) + lightColor * FdotL * m1;

    float sunsetFactor = LightVisibility * (1.0 - NightMultiplier);
    vec4 sunsetColor = mix(color, vec4(0.9, 0.1, 0.05, 1.0), sunsetFactor);
    color = mix(color, sunsetColor, m2);

    // Mie scattering approx
//    color.rgb += LightColor * min((1.0 / ((1 - RdotL) * 2.0 + 0.1)), 0.8) * (0.2 + 0.8 * LightVisibility);
    float f = 10.0 + 2000.0 * LightVisibility;
    color.rgb += LightColor * min((1.0 / ((1 - RdotL) * f + 1.0)), 1.0) * (0.5 + 0.5 * LightVisibility);

    return color;
}

vec3 getSunColor(const float RdotL) {
    float m = smoothstep(0.9991, 0.9993, RdotL) * LightVisibility;
    vec3 color = mix(vec3(0.0), LightColor, m);
    return color;
}

vec3 getMoonColor(const float RdotL) {
    vec3 color = LightColor * LightVisibility * (0.05 / ((1 - RdotL) * 200.0 + 1.0));

    return color;
}

void main() {
    //TODO precompute and optimize
    vec3 fragDir = normalize(fragPos);
    float RoUp = dot(fragDir, UpVector);
    RoUp = abs(RoUp);
    RoUp = max(RoUp, 0.0);

    float SoUp = max(dot(UpVector, LightDir), 0.0);

    float RdotL = max(dot(fragDir, LightDir), 0.0);

    fragColor = getSkyColor(RdotL, RoUp, SoUp);
    fragColor.rgb += NightMultiplier == 1.0  ? getMoonColor(RdotL) : getSunColor(RdotL);
}

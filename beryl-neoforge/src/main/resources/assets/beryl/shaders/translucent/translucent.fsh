#version 450

#include "materials.glsl"

layout(binding = 2) uniform sampler2D Sampler0;
layout(binding = 4) uniform sampler2D ShadowMap;

layout(binding = 0) uniform UniformBufferObject {
    mat4 MVP;
    mat4 ProjMat;
    mat4 ModelViewMat;
    mat4 LightSpaceMat;
    vec3 LightSpaceOffset;
    vec3 ViewPos;
};

layout(binding = 1) uniform UBO {
    vec4 ColorModulator;
    vec4 SkyColor;
    vec4 FogColor;
    vec3 UpVector;
    float FogStart;
    float FogEnd;
    float FogEnvStart;
    float FogEnvEnd;
    vec3 LightDir;
    vec3 LightColor;
    vec3 ViewPos2;
    float GameTime;
    float LightIntensity;
    float LightVisibility;
    float NightFactor;
    float AmbientLightFactor;
    float MinAmbientLight;
    float FogFactor;
    float ShadowTexelSize;
    float ShadowBias;
    float ShadowDistortion;
};

layout(location = 0) in float vertexDistance;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in vec3 normal;
layout(location = 3) in vec2 texCoord0;
layout(location = 4) in vec4 posLightSpace;
layout(location = 5) in vec3 fragPos;
layout(location = 6) in vec3 light;
layout(location = 7) in flat Material material;

layout(location = 0) out vec4 fragColor;

#include "fog2.glsl"
#include "lighting.glsl"
#include "shadow.glsl"

vec3 hash(vec3 a);

float fresnelSchlick(float cosTheta, float F0)
{
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}

vec3 waterNormal(sampler2D NoiseTex, vec3 worldPos, float gameTime) {
    worldPos *= 0.1;

    //    return texture(Sampler4, worldPos.xz).ggg - vec3(0.5);
    //    float noiseA = texture(Sampler4, worldPos.xz)

    float animation = gameTime * 40;
    //    vec2 animation = vec2(0.0);

    //    return texture(Sampler4, worldPos.xz + animation).ggg;
    //    return texture(Sampler4, worldPos.xz + animation).rrr;
    //    return texture(Sampler4, worldPos.xz).ggg;

    //    float n1 = texture(Sampler4, worldPos.xz + animation).r;
    //    float n2 = texture(Sampler4, (worldPos.xz + animation) * 5).r * 0.125;
    //    float n1 = (texture(Sampler4, worldPos.xz + animation).r - 0.5) * 2;
    //    float n2 = (texture(Sampler4, (worldPos.xz + animation) * 5).r - 0.5) * 0.125;
    //
    //    return vec3(n1 + n2);

    worldPos.xz += animation;
    float normalOffset = 0.25;
    //    float normalOffset = 0.005;
    //
    ////    float fresnel = pow(clamp(1.0 + dot(normalize(normal), normalize(viewPos)), 0.0, 1.0), 8.0);
    ////    float normalStrength = 0.35 * (1.0 - fresnel);
    float normalStrength = 0.1;

    float h1 = texture(NoiseTex, vec2(worldPos.x + normalOffset, worldPos.z)).g;
    float h2 = texture(NoiseTex, vec2(worldPos.x - normalOffset, worldPos.z)).g;
    float h3 = texture(NoiseTex, vec2(worldPos.x, worldPos.z + normalOffset)).g;
    float h4 = texture(NoiseTex, vec2(worldPos.x, worldPos.z - normalOffset)).g;

    float xDelta = (h2 - h1) / normalOffset;
    float yDelta = (h4 - h3) / normalOffset;

    //    vec3 normalMap = vec3(xDelta, yDelta, 1.0 - (xDelta * xDelta + yDelta * yDelta));
    //    return normalMap * normalStrength + vec3(0.0, 0.0, 1.0 - normalStrength);

    //    vec3 normalMap = vec3(xDelta, yDelta, 0.0);
    //    return normalMap * normalStrength;

    //    return vec3(xDelta, 0.0, yDelta);
    return normalize(vec3(normalStrength * xDelta, 1.0, normalStrength * yDelta));
}

vec3 getWaterNormal(vec3 pos, vec3 normal) {
    vec3 N;

    // Sine wave
    float m = cos(pos.x);
    float mN = 1 / (m + 0.001);
    N.y = mN;
    N.x = 1;
    N.z = 0;

    N = normalize(N);

    return N;
}

void main() {
    vec4 texColor = texture(Sampler0, texCoord0) * vertexColor;

    vec3 Normal = normal.xyz;

    //    //Add random noise to simulate waves
    //    Normal += hash(fragPosWS.xyz) * 0.002;
    //    Normal = normalize(Normal);

    //    Normal = getWaterNormal(fragPosWS, normal);

    //    Normal = waterNormal(fragPosWS);
    //    Normal = (ModelViewMat * vec4(Normal, 0.0)).xyz;

    vec4 color;
    float shadow = ShadowCalculation(ShadowMap, posLightSpace, ShadowTexelSize, ShadowBias);

    // Water
    if (material.id == 1) {
        #include "materials/water.glsl"
    }
    else if (material.id == 2) {
        #include "materials/glass.glsl"
    }
    else {
        vec3 viewDir = normalize(-fragPos);
        vec3 N = normal;
        vec3 V = viewDir;

        vec3 F0 = material.F0;
        float roughness = material.roughness;
        float metallic = material.metallic;

        vec3 albedo = texColor.rgb;

        vec3 radiance = LightColor;

        // Gradually reduce lighting when its low on the horizon
        radiance *= saturate((dot(UpVector, LightDir) - 0.04) * 50); // 1 / 0.02

        vec3 color1 = LightingSphereGGX2(viewDir, normal, LightDir, albedo, radiance, F0, roughness, metallic, material.lightingType);

        color.rgb = (color1 * shadow * light.z) + albedo * (0.3 * light.y + light.x * vec3(1.0, 0.7, 0.5));
        float NdotU = max(dot(N, UpVector), 0.0);
        NdotU = material.lightingType == 0 ? NdotU : 0.8;
        color.rgb += radiance * 0.05 * NdotU * albedo * shadow * light.z;

        if (material.lightEmission > 0.0) {
            color.rgb += max((texColor.rgb - vec3(0.05)), vec3(0.0)) * material.lightEmission;
        }

        color.a = texColor.a;
    }

//    vec3 normalVS = Normal;
//    vec3 vCamToSampleInVS = normalize(fragPos.xyz);
//    vec4 vReflectionInVS = vec4(reflect(vCamToSampleInVS, normalVS.xyz), 0.0);
//
//    vec3 viewDir = normalize(-fragPos);
//
//    // Sky reflection
//    float RoUp = dot(vReflectionInVS.xyz, UpVector);
//    RoUp = max(RoUp, 0.0);
//
//    //TODO precompute
//    float SoUp = max(dot(UpVector, LightDir), 0.0);
//    float RdotL =  max(dot(vReflectionInVS.xyz, LightDir), 0.0);
//    vec3 skyColor = getSkyColor(RdotL, RoUp, SoUp).rgb;
//
//    vec3 reflectedColor = skyColor;
//
//    vec4 color;
//
//    float roughness = 0.003;
//    float metallic = 0.0;
//    float ao = 1.0;
//    vec3 albedo = texColor.rgb * 0.1;
//
//    vec3 N = Normal;
//    vec3 V = viewDir;
//
//    vec3 F0 = vec3(0.04);
//
//    vec3 radiance = reflectedColor.rgb;
//
//    vec3 color1 = LightingGGX(viewDir, Normal, vReflectionInVS.xyz, albedo, reflectedColor.rgb, F0, roughness, metallic);
//
//    // Sun Lighting
//    radiance = LightColor;
//
//    roughness = 0.4;
////    roughness = 0.01;
//    vec3 L = LightDir;
//
//    vec3 R = vReflectionInVS.xyz;
//
//    //    vec3 centerToRay = dot(L, R) * (R - L);
//    vec3 centerToRay = (R - L);
//    float radius = 0.042;
//    // closest point
//    float d = length(centerToRay);
//    vec3 O = centerToRay * saturate(radius / d);
//    L = L + O;
//    L = normalize(L);
//
//    vec3 H = normalize(V + L);
//
//    float NdotL = max(dot(N, L), 0.0);
//    float NdotV = max(dot(N, V), 0.0);
//    vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);
//
//    vec3 specular = LightingSpecularGGX(viewDir, normal, L, H, NdotL, NdotV, radiance, F, roughness, metallic);
//
//    vec3 kD = vec3(1.0) - F;
//    vec3 diffuse = (kD * albedo * 0.31830988618) * radiance * NdotL;
//
//    vec3 color2 = diffuse + specular * shadow;
//
//    NdotV = abs(dot(N, V));
//    color.a = clamp((1.0 - NdotV) * 1.0, texColor.a * 0.6, 1.0);
////    color.rgb = (color2 * shadow * 0.8 + color1 * 0.8) * light.z + albedo * (0.2 * light.y + light.x * vec3(1.0, 0.7, 0.5));
////    color.rgb = (color1 * 0.8) * light.z + albedo * (0.2 * light.y + light.x * vec3(1.0, 0.7, 0.5));
//
//    color.rgb = (color1 + color2) * light.z;
//    color.rgb += texColor.rgb * 0.6 * (0.2 * light.y + light.x * vec3(1.0, 0.7, 0.5));



    atmospheric_fog(color, fragPos, vertexDistance, FogFactor);

    // Fog
    if (vertexDistance > 0.8 * FogEnd) {
        //TODO precompute and optimize
        vec3 fragDir = normalize(fragPos);
        float RoUp = dot(fragDir, UpVector);
        RoUp = abs(RoUp);

        float SoUp = max(dot(UpVector, LightDir), 0.0);
        float RdotL = max(dot(fragDir, LightDir), 0.0);
        vec4 fogColor = vec4(getSkyColor(RdotL, RoUp, SoUp).rgb, 1.0);
        fogColor.rgb += getMieScattering(RdotL);

        color = fog(color, vertexDistance, FogEnd, fogColor);
    }

    fragColor = color;
}

vec3 hash(vec3 a)
{
    a = fract(a * 0.8);
    a += dot(a, a.yxz + 1.0);
    //    return fract((a.xxy + a.yxx)*a.zyx);
    return fract(a.xyz * a.xyz + a.zxy);
}
#define PI 3.14159265359

const vec2 poissonDisk[4] = vec2[](
    vec2( -0.94201624, -0.39906216 ),
    vec2( 0.94558609, -0.76890725 ),
    vec2( -0.094184101, -0.92938870 ),
    vec2( 0.34495938, 0.29387760 )
);

vec3 distortShadowClipPos(vec3 shadowClipPos) {
//    float dist = max(length(shadowClipPos.xy) - 0.05f, 0.0f);
    float dist = length(shadowClipPos.xy);
    float distortionFactor = dist * ShadowDistortion; // distance from the center
    distortionFactor += (1.0f - ShadowDistortion);

    shadowClipPos.xy /= distortionFactor;
    shadowClipPos.z += 0.5;
    shadowClipPos.z *= 0.5;
    return shadowClipPos;
}

float ShadowCalculation(sampler2D ShadowMap, vec4 fragPosLightSpace, float texelSize, float bias)
{
    //    fragPosLightSpace.xyz = distortShadowClipPos(fragPosLightSpace.xyz);

    // perform perspective divide
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;

    float distX = abs(projCoords.x);
    float distY = abs(projCoords.y);
    float dist = max(distX, distY);

    // transform to [0,1] range
    projCoords.y = -projCoords.y;
    projCoords.xy = projCoords.xy * 0.5 + 0.5;

    if (projCoords.x < 0.0 || projCoords.x > 1.0
        || projCoords.y < 0.0 ||  projCoords.y > 1.0
        || projCoords.z < 0.0 || projCoords.z > 1.0)
    {
        return 1.0;
    }


    //    // get closest depth value from light's perspective (using [0,1] range fragPosLight as coords)
    //        float closestDepth = texture(ShadowMap, projCoords.xy).r;
    //        // get depth of current fragment from light's perspective
    //        float currentDepth = projCoords.z;
    //        // check whether current frag pos is in shadow
    //        float shadow = currentDepth - bias > closestDepth ? 0.0 : 1.0;

    //PCF
    float currentDepth = projCoords.z;
    float shadow = 0.0;
    //TODO texture size uniform
    float pcfDepth = 0.0;

//        //9 taps
//        for(int x = -1; x <= 1; ++x)
//        {
//            for(int y = -1; y <= 1; ++y)
//            {
//                pcfDepth = texture(ShadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
//                //            shadow += currentDepth - 0.005 > pcfDepth ? 0.0 : 1.0;
//                shadow += currentDepth - bias > pcfDepth ? 0.0 : 0.111111;
//            }
//        }

//    //16 taps
//    float x, y;
//    for(x = -1.5; x <= 1.5; x += 1.0)
//    {
//        for(y = -1.5; y <= 1.5; y += 1.0)
//        {
//            pcfDepth = texture(ShadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
//            //            pcfDepth = texelFetch(ShadowMap, ivec2((projCoords.xy + vec2(x, y) * texelSize) * textureSize(ShadowMap, 0)), 0).r;
//            //            shadow += currentDepth - 0.005 > pcfDepth ? 0.0 : 1.0;
//            shadow += currentDepth - bias > pcfDepth ? 0.0 : 0.0625;
//        }
//    }

    bias += 0.002 * dist * dist;

    // Poisson fixed disc 4 taps
    vec2 texelSizeM = 1.0 * vec2(texelSize);
    for (int i = 0 ; i < 4; i++){
        pcfDepth = texture(ShadowMap, projCoords.xy + poissonDisk[i] * texelSizeM).r;
        shadow += currentDepth - bias > pcfDepth ? 0.0 : 0.2;
        //        shadow += currentDepth - bias < pcfDepth ? 0.0 : 0.2;
    }
    pcfDepth = texture(ShadowMap, projCoords.xy).r;
    shadow += currentDepth - bias > pcfDepth ? 0.0 : 0.2;

    //Poisson disc randomly rotated
    //    float randomAngle = gl_FragCoord.x + gl_FragCoord.y * (2.0 * PI);
    //    vec2 randomBase = vec2(cos(randomAngle), sin(randomAngle));
    //    mat2 R = mat2(randomBase.x, randomBase.y, -randomBase.y, randomBase.x);
    //
    //    vec2 texelSizeM = 1.0 * texelSize;
    //    for (int i = 0 ; i < 4; i++){
    //        pcfDepth = texture(ShadowMap, projCoords.xy + R * poissonDisk[i] * texelSizeM).r;
    //        shadow += currentDepth - 0.0015 > pcfDepth ? 0.0 : 0.2;
    //    }
    //    pcfDepth = texture(ShadowMap, projCoords.xy).r;
    //    shadow += currentDepth - 0.0015 > pcfDepth ? 0.0 : 0.2;


    // fade on shadow map borders for smoother transition
    float intensity = 1 - clamp((dist - 0.9) / (1.0 - 0.9), 0.0, 1.0);
    shadow = 1 - shadow;
    shadow *= intensity;
    shadow = 1 - shadow;

    return shadow;
}
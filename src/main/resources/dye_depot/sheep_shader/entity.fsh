#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

#ifdef DISSOLVE
uniform sampler2D DissolveMaskSampler;
#endif

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
#ifdef PER_FACE_LIGHTING
in vec4 vertexPerFaceColorBack;
in vec4 vertexPerFaceColorFront;
#else
in vec4 vertexColor;
#endif

#ifndef EMISSIVE
in vec4 lightMapColor;
#endif

#ifndef NO_OVERLAY
in vec4 overlayColor;
#endif

in vec2 texCoord0;
in vec3 dyeDepotPosition;
in vec2 dyeDepotTexCoord;
in vec4 dyeDepotRawColor;

out vec4 fragColor;

const float DYE_DEPOT_SCALE_STEP = 1.0 / 128.0;
const vec3 DYE_DEPOT_MARKER = vec3(217.0, 229.0, 161.0) / 255.0;

bool dyeDepotMarkerMatches(vec4 sampleColor, vec3 expected) {
    return sampleColor.a < (0.5 / 255.0)
        && all(lessThan(abs(sampleColor.rgb - expected), vec3(0.5 / 255.0)));
}

int dyeDepotTextureKind() {
    ivec2 size = textureSize(Sampler0, 0);
    if (any(notEqual(size, ivec2(64, 32)))) {
        return 0;
    }
    if (!dyeDepotMarkerMatches(texelFetch(Sampler0, ivec2(63, 31), 0), DYE_DEPOT_MARKER)) {
        return 0;
    }
    vec4 kind = texelFetch(Sampler0, ivec2(62, 31), 0);
    if (dyeDepotMarkerMatches(kind, vec3(1.0, 2.0, 1.0) / 255.0)) return 1;
    if (dyeDepotMarkerMatches(kind, vec3(1.0, 2.0, 2.0) / 255.0)) return 2;
    if (dyeDepotMarkerMatches(kind, vec3(1.0, 2.0, 3.0) / 255.0)) return 3;
    if (dyeDepotMarkerMatches(kind, vec3(1.0, 2.0, 4.0) / 255.0)) return 4;
    if (dyeDepotMarkerMatches(kind, vec3(1.0, 2.0, 5.0) / 255.0)) return 5;
    return 0;
}

int dyeDepotTextureTag() {
    ivec2 texel = clamp(
        ivec2(floor(dyeDepotTexCoord * vec2(textureSize(Sampler0, 0)))),
        ivec2(0),
        textureSize(Sampler0, 0) - ivec2(1)
    );
    return int(floor(texelFetch(Sampler0, texel, 0).a * 255.0 + 0.5));
}

float dyeDepotExpectedArea(int kind) {
    if (kind != 1) {
        return 8.0;
    }
    int tag = dyeDepotTextureTag();
    if (tag == 250) return 11.52;
    if (tag == 251) return 12.5;
    if (tag == 252) return 11.6666666667;
    if (tag == 253) return 18.2083333333;
    if (tag == 254) return 15.4375;
    if (tag == 255) return 14.015625;
    return 0.0;
}

int dyeDepotScaleClass(float expectedArea) {
    vec3 positionX = dFdx(dyeDepotPosition);
    vec3 positionY = dFdy(dyeDepotPosition);
    vec2 textureX = dFdx(dyeDepotTexCoord);
    vec2 textureY = dFdy(dyeDepotTexCoord);
    float determinant = textureX.x * textureY.y - textureX.y * textureY.x;
    if (abs(determinant) < 1.0e-8 || expectedArea <= 0.0) {
        return 0;
    }
    vec3 positionU = (positionX * textureY.y - positionY * textureX.y) / determinant;
    vec3 positionV = (-positionX * textureY.x + positionY * textureX.x) / determinant;
    float renderedScale = sqrt(length(cross(positionU, positionV)) / expectedArea);
    int latticeStep = int(floor(log2(max(renderedScale, 1.0e-8)) / DYE_DEPOT_SCALE_STEP + 0.5));
    return ((latticeStep % 5) + 5) % 5;
}

int dyeDepotNearestVanillaColor(vec3 color) {
    const vec3 vanillaColors[16] = vec3[](
        vec3(230.0, 230.0, 230.0), vec3(186.0, 96.0, 21.0),
        vec3(149.0, 58.0, 141.0), vec3(43.0, 134.0, 163.0),
        vec3(190.0, 162.0, 45.0), vec3(96.0, 149.0, 23.0),
        vec3(182.0, 104.0, 127.0), vec3(53.0, 59.0, 61.0),
        vec3(117.0, 117.0, 113.0), vec3(16.0, 117.0, 117.0),
        vec3(102.0, 37.0, 138.0), vec3(45.0, 51.0, 127.0),
        vec3(98.0, 63.0, 37.0), vec3(70.0, 93.0, 16.0),
        vec3(132.0, 34.0, 28.0), vec3(21.0, 21.0, 24.0)
    );
    int nearest = 0;
    float nearestDistance = 1.0e30;
    for (int index = 0; index < 16; index++) {
        vec3 difference = color - vanillaColors[index] / 255.0;
        float distanceSquared = dot(difference, difference);
        if (distanceSquared < nearestDistance) {
            nearestDistance = distanceSquared;
            nearest = index;
        }
    }
    return nearest;
}

vec3 dyeDepotCustomColor(int index) {
    const vec3 customColors[16] = vec3[](
        vec3(92.0, 29.0, 14.0), vec3(191.0, 70.0, 75.0),
        vec3(167.0, 89.0, 66.0), vec3(38.0, 22.0, 65.0),
        vec3(15.0, 45.0, 75.0), vec3(57.0, 70.0, 100.0),
        vec3(105.0, 107.0, 31.0), vec3(161.0, 131.0, 0.0),
        vec3(168.0, 159.0, 122.0), vec3(35.0, 92.0, 77.0),
        vec3(42.0, 154.0, 93.0), vec3(70.0, 180.0, 153.0),
        vec3(27.0, 65.0, 15.0), vec3(37.0, 122.0, 28.0),
        vec3(155.0, 72.0, 24.0), vec3(183.0, 117.0, 69.0)
    );
    return customColors[clamp(index, 0, 15)] / 255.0;
}

void main() {
    vec4 color = texture(Sampler0, texCoord0);
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
#endif

#ifdef PER_FACE_LIGHTING
    vec4 faceVertexColor = gl_FrontFacing ? vertexPerFaceColorFront : vertexPerFaceColorBack;
#else
    vec4 faceVertexColor = vertexColor;
#endif

    int sheepTextureKind = dyeDepotTextureKind();
    if (sheepTextureKind != 0) {
        int residueClass = dyeDepotScaleClass(dyeDepotExpectedArea(sheepTextureKind));
        bool customUnsheared = residueClass == 1 || residueClass == 2;
        bool baseTexture = sheepTextureKind == 4 || sheepTextureKind == 5;
        bool innerTexture = baseTexture || sheepTextureKind == 3;
        if (customUnsheared && innerTexture && dyeDepotTextureTag() == 249) {
            discard;
        }
        if (residueClass != 0 && !baseTexture) {
            int donor = dyeDepotNearestVanillaColor(dyeDepotRawColor.rgb);
            int customIndex = residueClass == 2 || residueClass == 4 ? 15 : donor - 1;
            vec3 lightFactor = faceVertexColor.rgb / max(dyeDepotRawColor.rgb, vec3(1.0 / 255.0));
            faceVertexColor.rgb = dyeDepotCustomColor(customIndex) * lightFactor;
        }
        // The adult metric map uses near-opaque alpha values as private tags.
        color.a = 1.0;
    }

#ifdef DISSOLVE
    if (faceVertexColor.a < texture(DissolveMaskSampler, texCoord0).a) {
        discard;
    }
    // The dissolve effect entirely replaces translucency
    faceVertexColor.a = 1.0;
#endif

    color *= faceVertexColor * ColorModulator;
#ifndef NO_OVERLAY
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
#endif
#ifndef EMISSIVE
    color *= lightMapColor;
#endif

    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}

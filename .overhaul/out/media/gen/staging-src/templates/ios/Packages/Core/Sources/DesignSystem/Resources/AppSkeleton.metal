#include <metal_stdlib>
#include <SwiftUI/SwiftUI_Metal.h>
using namespace metal;

// Draws a redacted view's placeholders in the skeleton colour, lit where the shimmer band crosses
// them. `appRedacted(_:)` applies it as a layer effect; Components/Skeleton has the geometry.
//
// SwiftUI draws a placeholder in the content's own foreground style at an opacity of its own, so a
// pixel's alpha only says how much of a bone covers it relative to that opacity. Dividing by the
// strongest alpha `reach` points around the pixel recovers the coverage, antialiased edges
// included, and every bone takes the token colours whatever style its text had.
//
// dot(position, axis) + offset is the pixel's distance past the band's leading edge. It is the
// same for a given point of the window whichever view draws it, so every placeholder samples one
// band. Across the band the colour rises linearly from bone to highlight at its middle and falls
// back, as the Android template's gradient does. A band width of zero means no band.
[[ stitchable ]] half4 appSkeleton(
    float2 position,
    SwiftUI::Layer layer,
    float2 axis,
    float offset,
    float bandWidth,
    half4 bone,
    half4 highlight,
    float reach
) {
    half alpha = layer.sample(position).a;
    if (alpha == 0.0h) {
        return half4(0.0h);
    }
    half solid = max(
        max(layer.sample(position + float2(reach, 0.0)).a, layer.sample(position - float2(reach, 0.0)).a),
        max(layer.sample(position + float2(0.0, reach)).a, layer.sample(position - float2(0.0, reach)).a)
    );
    half coverage = alpha / max(alpha, solid);

    half lift = 0.0h;
    if (bandWidth > 0.0) {
        float across = (dot(position, axis) + offset) / bandWidth;
        lift = half(saturate(1.0 - abs(2.0 * across - 1.0)));
    }
    return mix(bone, highlight, lift) * coverage;
}

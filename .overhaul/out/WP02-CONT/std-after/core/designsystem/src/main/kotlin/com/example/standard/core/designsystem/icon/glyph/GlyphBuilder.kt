package com.example.standard.core.designsystem.icon.glyph

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.unit.dp

internal const val StrokeWidth = 1.8f
internal const val BoldStrokeWidth = 2.6f

/** Dots are drawn as a zero-length round-capped stroke, so the width *is* the diameter. */
internal const val DotWidth = 2.4f

/**
 * Built the first time something draws it, not when the set is first touched: building all of them
 * at once would land on the first frame. [autoMirror] flips a directional glyph in right-to-left
 * layouts, so a back arrow points back.
 */
internal fun icon(
    name: String,
    autoMirror: Boolean = false,
    block: ImageVector.Builder.() -> Unit,
): Lazy<ImageVector> = lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror,
    ).apply(block).build()
}

internal fun ImageVector.Builder.stroke(
    width: Float = StrokeWidth,
    path: PathBuilder.() -> Unit,
): ImageVector.Builder = addPath(
    pathData = PathData(path),
    stroke = SolidColor(Color.Black),
    strokeLineWidth = width,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
)

internal fun ImageVector.Builder.fill(
    evenOdd: Boolean = false,
    path: PathBuilder.() -> Unit,
): ImageVector.Builder = addPath(
    pathData = PathData(path),
    pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
    fill = SolidColor(Color.Black),
)

/** Two half-arcs, because the vector format has no circle primitive. */
internal fun PathBuilder.circle(centerX: Float, centerY: Float, radius: Float) {
    moveTo(centerX - radius, centerY)
    arcTo(radius, radius, 0f, true, true, centerX + radius, centerY)
    arcTo(radius, radius, 0f, true, true, centerX - radius, centerY)
    close()
}

internal fun PathBuilder.dot(x: Float, y: Float) {
    moveTo(x, y)
    lineTo(x + 0.01f, y)
}

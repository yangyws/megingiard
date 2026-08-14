package com.stormpanda.megingiard.macropad

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A square selection expressed in normalised stage units.
 */
data class CropSelection(
    val centerX: Float,
    val centerY: Float,
    val size: Float,
) {
    val left: Float get() = centerX - size / 2f
    val top: Float get() = centerY - size / 2f
}

/** Which corner of a [CropSelection] a resize drag grabbed. */
enum class CropCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    ;

    internal val signX: Float get() = if (this == TOP_LEFT || this == BOTTOM_LEFT) -1f else 1f
    internal val signY: Float get() = if (this == TOP_LEFT || this == TOP_RIGHT) -1f else 1f
}

data class CropTransform(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
)

object CropSelectionMath {
    const val MAX_ZOOM = 5f

    fun imageExtents(
        imageWidth: Float,
        imageHeight: Float,
        aspectRatio: Float,
    ): Pair<Float, Float> {
        if (imageWidth <= 0f || imageHeight <= 0f || aspectRatio <= 0f) return 1f to 1f
        val imageAspect = imageWidth / imageHeight
        return if (imageAspect >= aspectRatio) {
            1f to (aspectRatio / imageAspect).coerceIn(0f, 1f)
        } else {
            (imageAspect / aspectRatio).coerceIn(0f, 1f) to 1f
        }
    }

    fun fillSize(
        widthFraction: Float,
        heightFraction: Float,
    ): Float = min(widthFraction, heightFraction).coerceIn(0f, 1f)

    fun minSize(
        widthFraction: Float,
        heightFraction: Float,
    ): Float = fillSize(widthFraction, heightFraction) / MAX_ZOOM

    fun maxSize(
        widthFraction: Float,
        heightFraction: Float,
        allowMargins: Boolean,
    ): Float = if (allowMargins) 1f else fillSize(widthFraction, heightFraction)

    fun clamp(
        selection: CropSelection,
        widthFraction: Float,
        heightFraction: Float,
        allowMargins: Boolean,
    ): CropSelection {
        val size =
            selection.size.coerceIn(
                minSize(widthFraction, heightFraction),
                maxSize(widthFraction, heightFraction, allowMargins),
            )
        val boundW = if (allowMargins) 1f else widthFraction
        val boundH = if (allowMargins) 1f else heightFraction
        val marginX = max(0f, (boundW - size) / 2f)
        val marginY = max(0f, (boundH - size) / 2f)
        return CropSelection(
            centerX = selection.centerX.coerceIn(HALF - marginX, HALF + marginX),
            centerY = selection.centerY.coerceIn(HALF - marginY, HALF + marginY),
            size = size,
        )
    }

    fun maxSelection(
        widthFraction: Float,
        heightFraction: Float,
        allowMargins: Boolean,
    ): CropSelection =
        clamp(
            CropSelection(HALF, HALF, maxSize(widthFraction, heightFraction, allowMargins)),
            widthFraction,
            heightFraction,
            allowMargins,
        )

    fun move(
        selection: CropSelection,
        dx: Float,
        dy: Float,
        widthFraction: Float,
        heightFraction: Float,
        allowMargins: Boolean,
    ): CropSelection =
        clamp(
            selection.copy(centerX = selection.centerX + dx, centerY = selection.centerY + dy),
            widthFraction,
            heightFraction,
            allowMargins,
        )

    fun resize(
        selection: CropSelection,
        corner: CropCorner,
        dx: Float,
        dy: Float,
        widthFraction: Float,
        heightFraction: Float,
        allowMargins: Boolean,
    ): CropSelection {
        val deltaW = corner.signX * dx
        val deltaH = corner.signY * dy
        val delta = if (abs(deltaW) >= abs(deltaH)) deltaW else deltaH

        val anchorX = selection.centerX - corner.signX * selection.size / 2f
        val anchorY = selection.centerY - corner.signY * selection.size / 2f

        val lowest = minSize(widthFraction, heightFraction)
        val highest = maxSize(widthFraction, heightFraction, allowMargins)
        val boundW = if (allowMargins) 1f else widthFraction
        val boundH = if (allowMargins) 1f else heightFraction
        val reachX = if (corner.signX > 0f) HALF + boundW / 2f - anchorX else anchorX - (HALF - boundW / 2f)
        val reachY = if (corner.signY > 0f) HALF + boundH / 2f - anchorY else anchorY - (HALF - boundH / 2f)
        val ceiling = if (allowMargins) highest else min(highest, max(lowest, min(reachX, reachY)))

        val size = (selection.size + delta).coerceIn(lowest, ceiling)
        return clamp(
            CropSelection(
                centerX = anchorX + corner.signX * size / 2f,
                centerY = anchorY + corner.signY * size / 2f,
                size = size,
            ),
            widthFraction,
            heightFraction,
            allowMargins,
        )
    }

    fun toTransform(
        selection: CropSelection,
        widthFraction: Float,
        heightFraction: Float,
    ): CropTransform {
        val size = selection.size
        if (size <= 0f) return CropTransform(1f, 0f, 0f)
        return CropTransform(
            scale = fillSize(widthFraction, heightFraction) / size,
            offsetX = (HALF - selection.centerX) / size,
            offsetY = (HALF - selection.centerY) / size,
        )
    }

    fun fromTransform(
        transform: CropTransform,
        widthFraction: Float,
        heightFraction: Float,
        allowMargins: Boolean,
    ): CropSelection {
        val scale = if (transform.scale > 0f && transform.scale.isFinite()) transform.scale else 1f
        val size = fillSize(widthFraction, heightFraction) / scale
        return clamp(
            CropSelection(
                centerX = HALF - transform.offsetX * size,
                centerY = HALF - transform.offsetY * size,
                size = size,
            ),
            widthFraction,
            heightFraction,
            allowMargins,
        )
    }

    fun imageRectInOutput(
        selection: CropSelection,
        widthFraction: Float,
        heightFraction: Float,
    ): FloatArray {
        val size = selection.size
        if (size <= 0f) return floatArrayOf(0f, 0f, 1f, 1f)
        return floatArrayOf(
            (HALF - widthFraction / 2f - selection.left) / size,
            (HALF - heightFraction / 2f - selection.top) / size,
            widthFraction / size,
            heightFraction / size,
        )
    }

    private const val HALF = 0.5f
}

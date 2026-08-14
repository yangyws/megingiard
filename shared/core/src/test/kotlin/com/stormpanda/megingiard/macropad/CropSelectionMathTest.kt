package com.stormpanda.megingiard.macropad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

class CropSelectionMathTest {
    private val EPS = 0.001f

    @Test
    fun testExtentsSquare() {
        val (w, h) = CropSelectionMath.imageExtents(100f, 100f, 1f)
        assertEquals(1f, w, EPS)
        assertEquals(1f, h, EPS)
    }

    @Test
    fun testExtentsWide() {
        val (w, h) = CropSelectionMath.imageExtents(200f, 100f, 1f)
        assertEquals(1f, w, EPS)
        assertEquals(0.5f, h, EPS)
    }

    @Test
    fun testExtentsTall() {
        val (w, h) = CropSelectionMath.imageExtents(100f, 200f, 1f)
        assertEquals(0.5f, w, EPS)
        assertEquals(1f, h, EPS)
    }

    @Test
    fun testDominantDragAxisWins() {
        val (w, h) = CropSelectionMath.imageExtents(100f, 100f, 1f)
        val start = CropSelection(0.5f, 0.5f, 0.4f)
        val resized =
            CropSelectionMath.resize(start, CropCorner.BOTTOM_RIGHT, dx = 0.2f, dy = 0.05f, w, h, allowMargins = false)
        assertEquals(0.6f, resized.size, EPS)
        assertEquals(0.3f, resized.left, EPS)
        assertEquals(0.3f, resized.top, EPS)
    }

    @Test
    fun testFillBaselineTransform() {
        val (w, h) = CropSelectionMath.imageExtents(200f, 100f, 1f)
        val baseline = CropSelection(0.5f, 0.5f, CropSelectionMath.fillSize(w, h))
        val transform = CropSelectionMath.toTransform(baseline, w, h)
        assertEquals(1f, transform.scale, EPS)
        assertEquals(0f, transform.offsetX, EPS)
        assertEquals(0f, transform.offsetY, EPS)
    }

    @Test
    fun testStoredCropReopens() {
        val (w, h) = CropSelectionMath.imageExtents(160f, 90f, aspectRatio = 4f / 3f)
        val original = CropSelection(0.42f, 0.5f, 0.55f)
        val reopened =
            CropSelectionMath.fromTransform(
                CropSelectionMath.toTransform(original, w, h),
                w,
                h,
                allowMargins = true,
            )
        assertEquals(original.centerX, reopened.centerX, EPS)
        assertEquals(original.centerY, reopened.centerY, EPS)
        assertEquals(original.size, reopened.size, EPS)
    }

    @Test
    fun testAllowMarginsFreeVerticalAndHorizontalMovement() {
        val (w, h) = CropSelectionMath.imageExtents(200f, 100f, 1f) // wide image: w=1.0, h=0.5
        val shrunk = CropSelection(0.5f, 0.5f, 0.4f)

        // Move vertically up and down
        val movedUp = CropSelectionMath.move(shrunk, dx = 0f, dy = -0.3f, w, h, allowMargins = true)
        assertEquals(0.2f, movedUp.centerY, EPS)
        assertEquals(0.0f, movedUp.top, EPS)

        val movedDown = CropSelectionMath.move(shrunk, dx = 0f, dy = 0.3f, w, h, allowMargins = true)
        assertEquals(0.8f, movedDown.centerY, EPS)
        assertEquals(1.0f, movedDown.centerY + movedDown.size / 2f, EPS)

        // Move horizontally left and right
        val movedLeft = CropSelectionMath.move(shrunk, dx = -0.3f, dy = 0f, w, h, allowMargins = true)
        assertEquals(0.2f, movedLeft.centerX, EPS)

        val movedRight = CropSelectionMath.move(shrunk, dx = 0.3f, dy = 0f, w, h, allowMargins = true)
        assertEquals(0.8f, movedRight.centerX, EPS)
    }
}


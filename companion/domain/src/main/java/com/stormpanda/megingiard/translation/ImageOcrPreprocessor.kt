package com.stormpanda.megingiard.translation

import com.stormpanda.megingiard.AppLog

private const val TAG = "ImageOcrPreprocessor"
private const val LUMA_R = 0.299f
private const val LUMA_G = 0.587f
private const val LUMA_B = 0.114f

/**
 * Image processing utilities for game text extraction and binarization.
 * Operates purely on raw ARGB_8888 pixel buffers without requiring Android UI dependencies.
 */
object ImageOcrPreprocessor {

    data class BoundingRect(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    ) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
    }

    /**
     * Converts an ARGB pixel buffer to a binary mask using Otsu's adaptive thresholding
     * with support for colored fonts, gradients, and contrasting text edges.
     */
    fun binarize(
        pixels: IntArray,
        width: Int,
        height: Int,
        threshold: Int = -1,
        invertIfDarkText: Boolean = true,
    ): BooleanArray {
        val count = width * height
        if (count <= 0) return BooleanArray(0)

        val lumas = IntArray(count)
        val histogram = IntArray(256)

        var minLuma = 255
        var maxLuma = 0

        for (i in 0 until count) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF

            // Include max color channel difference for colored fonts on neutral backgrounds
            val maxC = kotlin.math.max(r, kotlin.math.max(g, b))
            val minC = kotlin.math.min(r, kotlin.math.min(g, b))
            val chroma = maxC - minC
            val standardLuma = (LUMA_R * r + LUMA_G * g + LUMA_B * b).toInt()

            // Enhance luma with chroma so colored letters (yellow, cyan, red, gold) stand out
            val luma = if (chroma > 30) {
                ((standardLuma * 0.6f) + (maxC * 0.4f)).toInt().coerceIn(0, 255)
            } else {
                standardLuma.coerceIn(0, 255)
            }

            lumas[i] = luma
            histogram[luma]++
            if (luma < minLuma) minLuma = luma
            if (luma > maxLuma) maxLuma = luma
        }

        // Determine threshold: use Otsu's method if threshold < 0
        val effectiveThreshold = if (threshold in 0..255) {
            threshold
        } else {
            calculateOtsuThreshold(histogram, count, minLuma, maxLuma)
        }

        val binary = BooleanArray(count)
        var whiteCount = 0

        for (i in 0 until count) {
            val isWhite = lumas[i] >= effectiveThreshold
            binary[i] = isWhite
            if (isWhite) whiteCount++
        }

        // If more than 50% of the image is white (light background with dark text), invert it
        if (invertIfDarkText && whiteCount > count / 2) {
            AppLog.d(TAG, "Inverting light background (white pixels=${whiteCount}/$count, threshold=$effectiveThreshold)")
            for (i in 0 until count) {
                binary[i] = !binary[i]
            }
        } else {
            AppLog.d(TAG, "Binarized with Otsu threshold=$effectiveThreshold (range $minLuma..$maxLuma, white=$whiteCount/$count)")
        }

        return binary
    }

    private fun calculateOtsuThreshold(
        histogram: IntArray,
        total: Int,
        minL: Int,
        maxL: Int,
    ): Int {
        if (minL >= maxL) return minL

        var sum = 0.0
        for (i in 0..255) {
            sum += i * histogram[i]
        }

        var sumB = 0.0
        var wB = 0
        var maxVariance = 0.0
        var bestThreshold = (minL + maxL) / 2

        for (t in minL until maxL) {
            wB += histogram[t]
            if (wB == 0) continue
            val wF = total - wB
            if (wF == 0) break

            sumB += t * histogram[t]
            val mB = sumB / wB
            val mF = (sum - sumB) / wF

            val betweenVariance = wB.toDouble() * wF.toDouble() * (mB - mF) * (mB - mF)
            if (betweenVariance > maxVariance) {
                maxVariance = betweenVariance
                bestThreshold = t + 1
            }
        }

        return bestThreshold
    }

    /**
     * Segments text line regions using horizontal and vertical projection profiles.
     */
    fun segmentTextRegions(
        binary: BooleanArray,
        width: Int,
        height: Int,
        minLineHeight: Int = 6,
    ): List<BoundingRect> {
        val horizontalProjection = IntArray(height)

        for (y in 0 until height) {
            var rowSum = 0
            val rowOffset = y * width
            for (x in 0 until width) {
                if (binary[rowOffset + x]) rowSum++
            }
            horizontalProjection[y] = rowSum
        }

        val regions = mutableListOf<BoundingRect>()
        var inLine = false
        var lineTop = 0

        for (y in 0 until height) {
            val hasContent = horizontalProjection[y] > (width * 0.01f).toInt()
            if (hasContent && !inLine) {
                inLine = true
                lineTop = y
            } else if (!hasContent && inLine) {
                inLine = false
                val lineHeight = y - lineTop
                if (lineHeight >= minLineHeight) {
                    // Find horizontal bounds for this line
                    var minX = width
                    var maxX = 0
                    for (ly in lineTop until y) {
                        val rowOffset = ly * width
                        for (x in 0 until width) {
                            if (binary[rowOffset + x]) {
                                if (x < minX) minX = x
                                if (x > maxX) maxX = x
                            }
                        }
                    }
                    if (maxX > minX) {
                        regions.add(BoundingRect(left = minX, top = lineTop, right = maxX + 1, bottom = y))
                    }
                }
            }
        }

        if (inLine && (height - lineTop) >= minLineHeight) {
            var minX = width
            var maxX = 0
            for (ly in lineTop until height) {
                val rowOffset = ly * width
                for (x in 0 until width) {
                    if (binary[rowOffset + x]) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                    }
                }
            }
            if (maxX > minX) {
                regions.add(BoundingRect(left = minX, top = lineTop, right = maxX + 1, bottom = height))
            }
        }

        AppLog.d(TAG, "Segmented ${regions.size} text region(s) in $width x $height")
        return regions
    }
}

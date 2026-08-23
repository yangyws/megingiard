package com.stormpanda.megingiard.translation

import com.stormpanda.megingiard.AppLog

private const val TAG = "PixelFontMatcher"
private const val GLYPH_SIZE = 8

/**
 * Fast Tier A matcher for retro 8x8 bitmap pixel fonts.
 */
object PixelFontMatcher {

    /**
     * Attempts to recognize characters in a binarized region by stepping through 8x8 cells.
     */
    fun matchRegion(
        binary: BooleanArray,
        imageWidth: Int,
        rect: ImageOcrPreprocessor.BoundingRect,
    ): String {
        val result = StringBuilder()
        val regionWidth = rect.width
        val regionHeight = rect.height

        val minHeight = 4
        val minWidth = 4
        if (regionHeight < minHeight || regionWidth < minWidth) return ""

        val alignedLeft = kotlin.math.max(0, rect.left - kotlin.math.max(0, (GLYPH_SIZE - regionWidth) / 2))
        val alignedRight = kotlin.math.max(alignedLeft + GLYPH_SIZE, rect.right)

        val cell = BooleanArray(GLYPH_SIZE * GLYPH_SIZE)
        var x = alignedLeft
        val totalPixels = binary.size

        while (x + GLYPH_SIZE <= alignedRight || (x == alignedLeft && x + GLYPH_SIZE <= imageWidth)) {
            // Extract 8x8 binary cell
            var hasAnyPixel = false
            for (cy in 0 until GLYPH_SIZE) {
                val rowOffset = (rect.top + cy) * imageWidth
                val cellRowOffset = cy * GLYPH_SIZE
                for (cx in 0 until GLYPH_SIZE) {
                    val pxIndex = rowOffset + x + cx
                    val px = if (pxIndex in 0 until totalPixels) binary[pxIndex] else false
                    cell[cellRowOffset + cx] = px
                    if (px) hasAnyPixel = true
                }
            }

            if (!hasAnyPixel) {
                // Space
                if (result.isNotEmpty() && result.last() != ' ') {
                    result.append(' ')
                }
                x += GLYPH_SIZE
                continue
            }

            val sig = PixelFontGlyphMap.computeSignature8x8(cell)
            val matched = PixelFontGlyphMap.matchGlyph(sig)
            if (matched != null) {
                result.append(matched)
            }
            x += GLYPH_SIZE
        }

        val recognized = result.toString().trim()
        AppLog.d(TAG, "Recognized pixel font text: '$recognized' in region $rect")
        return recognized
    }
}

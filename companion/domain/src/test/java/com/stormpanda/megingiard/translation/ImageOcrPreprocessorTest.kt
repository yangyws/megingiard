package com.stormpanda.megingiard.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageOcrPreprocessorTest {

    @Test
    fun testBinarizeSimpleImage() {
        val width = 4
        val height = 4
        // 2x2 white in top-left, rest black
        val pixels = IntArray(16) { 0xFF000000.toInt() }
        pixels[0] = 0xFFFFFFFF.toInt()
        pixels[1] = 0xFFFFFFFF.toInt()
        pixels[4] = 0xFFFFFFFF.toInt()
        pixels[5] = 0xFFFFFFFF.toInt()

        val binary = ImageOcrPreprocessor.binarize(pixels, width, height, threshold = 128, invertIfDarkText = false)
        assertTrue(binary[0])
        assertTrue(binary[1])
        assertTrue(binary[4])
        assertTrue(binary[5])
        assertTrue(!binary[2])
    }

    @Test
    fun testSegmentTextRegions() {
        val width = 16
        val height = 16
        val binary = BooleanArray(256) { false }

        // Fill a 16x8 block in middle (y = 4..11)
        for (y in 4 until 12) {
            for (x in 2 until 14) {
                binary[y * width + x] = true
            }
        }

        val regions = ImageOcrPreprocessor.segmentTextRegions(binary, width, height, minLineHeight = 4)
        assertEquals(1, regions.size)
        assertEquals(4, regions[0].top)
        assertEquals(12, regions[0].bottom)
    }
}

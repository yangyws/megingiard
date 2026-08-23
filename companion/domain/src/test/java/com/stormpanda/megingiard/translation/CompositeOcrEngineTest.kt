package com.stormpanda.megingiard.translation

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositeOcrEngineTest {

    @Test
    fun testEmptyImageReturnsEmptyList() = runTest {
        val engine = CompositeOcrEngine()
        val result = engine.recognize(IntArray(0), 0, 0)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testDetectPixelFontGlyph() = runTest {
        val width = 16
        val height = 16
        val pixels = IntArray(256) { 0xFF000000.toInt() }

        // Draw glyph '0' signature: 0x3C666E7666663C00
        val sig0 = 0x3C666E7666663C00UL.toLong()
        for (row in 0 until 8) {
            val byte = ((sig0 ushr (56 - row * 8)) and 0xFF).toInt()
            for (col in 0 until 8) {
                if ((byte and (1 shl (7 - col))) != 0) {
                    val x = col
                    val y = row + 4
                    pixels[y * width + x] = 0xFFFFFFFF.toInt()
                }
            }
        }

        val engine = CompositeOcrEngine()
        val blocks = engine.recognize(pixels, width, height, "ja")
        assertTrue(!blocks.isEmpty())
        assertEquals("0", blocks[0].text)
    }
}

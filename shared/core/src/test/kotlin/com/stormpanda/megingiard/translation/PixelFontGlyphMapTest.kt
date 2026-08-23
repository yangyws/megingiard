package com.stormpanda.megingiard.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PixelFontGlyphMapTest {

    @Test
    fun testMatchPredefinedGlyphs() {
        val sig0 = 0x3C666E7666663C00UL.toLong()
        val match = PixelFontGlyphMap.matchGlyph(sig0)
        assertNotNull(match)
        assertEquals('0', match)
    }

    @Test
    fun testComputeSignatureAndMatch() {
        val grid = BooleanArray(64) { false }
        // Set first bit
        grid[0] = true
        val sig = PixelFontGlyphMap.computeSignature8x8(grid)
        assertEquals(1L shl 63, sig)

        PixelFontGlyphMap.registerGlyph(sig, 'X')
        assertEquals('X', PixelFontGlyphMap.matchGlyph(sig))
    }

    @Test
    fun testUnmatchedSignatureReturnsNull() {
        assertNull(PixelFontGlyphMap.matchGlyph(0x123456789ABCDEF0UL.toLong()))
    }
}

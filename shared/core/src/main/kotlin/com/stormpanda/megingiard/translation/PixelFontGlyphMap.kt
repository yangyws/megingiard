package com.stormpanda.megingiard.translation

/**
 * Predefined 8x8 / 16x16 bitmask glyph signatures for retro pixel fonts (Tier A).
 */
object PixelFontGlyphMap {

    /**
     * Map of 64-bit row-major bitmask hash to character (for 8x8 font grid).
     */
    private val GLYPH_8X8_MAP = mutableMapOf<Long, Char>()

    init {
        // Register standard numbers 0-9 in classic 8x8 bitmap representation
        registerGlyph(0x3C666E7666663C00UL.toLong(), '0')
        registerGlyph(0x181C181818187E00UL.toLong(), '1')
        registerGlyph(0x3C66060C18307E00UL.toLong(), '2')
        registerGlyph(0x7E0C180C06663C00UL.toLong(), '3')
        registerGlyph(0x0C1C3C6C7E0C0C00UL.toLong(), '4')
        registerGlyph(0x7E607C0606663C00UL.toLong(), '5')
        registerGlyph(0x1C30607C66663C00UL.toLong(), '6')
        registerGlyph(0x7E060C1830303000UL.toLong(), '7')
        registerGlyph(0x3C66663C66663C00UL.toLong(), '8')
        registerGlyph(0x3C66663E060C3800UL.toLong(), '9')

        // Register Japanese Kana (Katakana: ア, イ, ウ, エ, オ, etc.)
        registerGlyph(0x7E060C1830604000UL.toLong(), 'ア')
        registerGlyph(0x2030282422200000UL.toLong(), 'イ')
        registerGlyph(0x103E040810204000UL.toLong(), 'ウ')
        registerGlyph(0x7E18181818187E00UL.toLong(), 'エ')
        registerGlyph(0x103E101018244200UL.toLong(), 'オ')
    }

    fun registerGlyph(signature: Long, character: Char) {
        GLYPH_8X8_MAP[signature] = character
    }

    /**
     * Look up a character by its 64-bit binary bitmap signature.
     */
    fun matchGlyph(signature: Long): Char? {
        return GLYPH_8X8_MAP[signature]
    }

    /**
     * Computes a 64-bit signature from an 8x8 binary boolean grid.
     */
    fun computeSignature8x8(grid: BooleanArray): Long {
        if (grid.size < 64) return 0L
        var sig = 0L
        for (i in 0 until 64) {
            if (grid[i]) {
                sig = sig or (1L shl (63 - i))
            }
        }
        return sig
    }
}

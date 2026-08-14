package com.stormpanda.megingiard.macropad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PadGlyphRulesTest {
    @Test
    fun testDefaultEnlargeIcon() {
        assertTrue(PadGlyphRules.defaultEnlargeIcon(isTableLayout = true))
        assertFalse(PadGlyphRules.defaultEnlargeIcon(isTableLayout = false))
    }

    @Test
    fun testPredatingButtonDefaultsToFalse() {
        assertFalse(PadButton(id = "a", label = "A", posX = 0.5f, posY = 0.5f, action = PadAction.ScrollWheel).enlargeIcon)
    }

    @Test
    fun testShowsEnlargeIcon() {
        assertTrue(PadGlyphRules.showsEnlargeIcon(iconName = "home", imageAssetId = null))
        assertFalse(PadGlyphRules.showsEnlargeIcon(iconName = null, imageAssetId = null))
        assertFalse(PadGlyphRules.showsEnlargeIcon(iconName = null, imageAssetId = "abc"))
        assertFalse(PadGlyphRules.showsEnlargeIcon(iconName = "home", imageAssetId = "abc"))
    }

    @Test
    fun testGlyphSizeDp() {
        assertEquals(44f, PadGlyphRules.glyphSizeDp(44f, faceSizeDp = 120f, enlarge = false), 0f)
        assertEquals(60f, PadGlyphRules.glyphSizeDp(44f, faceSizeDp = 60f, enlarge = true), 0f)
        assertEquals(100f, PadGlyphRules.glyphSizeDp(24f, faceSizeDp = 100f, enlarge = true), 0f)
        assertEquals(60f, PadGlyphRules.glyphSizeDp(44f, faceSizeDp = 60f, enlarge = false, fullBleed = true), 0f)
        assertEquals(18f, PadGlyphRules.glyphSizeDp(18f, faceSizeDp = null, enlarge = true), 0f)
        assertEquals(18f, PadGlyphRules.glyphSizeDp(18f, faceSizeDp = 0f, enlarge = true), 0f)
        assertEquals(46f, PadGlyphRules.glyphSizeDp(24f, faceSizeDp = 64f, enlarge = true, reserveDp = 18f), 0f)
        assertEquals(24f, PadGlyphRules.glyphSizeDp(24f, faceSizeDp = 26f, enlarge = true, reserveDp = 18f), 0f)
        assertEquals(24f, PadGlyphRules.glyphSizeDp(24f, faceSizeDp = 10f, enlarge = true), 0f)
        assertEquals(64f, PadGlyphRules.glyphSizeDp(24f, faceSizeDp = 64f, enlarge = true, reserveDp = -10f), 0f)
    }
}

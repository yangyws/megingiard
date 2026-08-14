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
        assertEquals(44f, PadGlyphRules.glyphSizeDp(44f, faceSizeDp = 120f, enlarge = false), 0.01f)
        assertEquals(45.6f, PadGlyphRules.glyphSizeDp(44f, faceSizeDp = 60f, enlarge = true), 0.01f)
        assertEquals(76f, PadGlyphRules.glyphSizeDp(24f, faceSizeDp = 100f, enlarge = true), 0.01f)
        assertEquals(55.2f, PadGlyphRules.glyphSizeDp(44f, faceSizeDp = 60f, enlarge = false, fullBleed = true), 0.01f)
        assertEquals(18f, PadGlyphRules.glyphSizeDp(18f, faceSizeDp = null, enlarge = true), 0.01f)
        assertEquals(18f, PadGlyphRules.glyphSizeDp(18f, faceSizeDp = 0f, enlarge = true), 0.01f)
        assertEquals(34.96f, PadGlyphRules.glyphSizeDp(24f, faceSizeDp = 64f, enlarge = true, reserveDp = 18f), 0.01f)
        assertEquals(24f, PadGlyphRules.glyphSizeDp(24f, faceSizeDp = 26f, enlarge = true, reserveDp = 18f), 0.01f)
        assertEquals(24f, PadGlyphRules.glyphSizeDp(24f, faceSizeDp = 10f, enlarge = true), 0.01f)
        assertEquals(48.64f, PadGlyphRules.glyphSizeDp(24f, faceSizeDp = 64f, enlarge = true, reserveDp = -10f), 0.01f)
    }
}

package com.stormpanda.megingiard.macropad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PadGlyphRulesTest {
    @Test
    fun glyphSizeDp_nullOrNonPositiveFaceSize_returnsDefault() {
        assertEquals(24f, PadGlyphRules.glyphSizeDp(24f, null, enlarge = true, fullBleed = true), 0.001f)
        assertEquals(24f, PadGlyphRules.glyphSizeDp(24f, 0f, enlarge = true, fullBleed = true), 0.001f)
        assertEquals(24f, PadGlyphRules.glyphSizeDp(24f, -10f, enlarge = true, fullBleed = true), 0.001f)
    }

    @Test
    fun glyphSizeDp_disabledEnlargeAndFullBleed_returnsDefault() {
        assertEquals(24f, PadGlyphRules.glyphSizeDp(24f, 80f, enlarge = false, fullBleed = false), 0.001f)
    }

    @Test
    fun glyphSizeDp_enlargeActive_scalesProportionally() {
        val freeResult = PadGlyphRules.glyphSizeDp(24f, 80f, enlarge = true, fullBleed = false, isTableLayout = false)
        assertEquals(80f * PadGlyphRules.FREE_ENLARGED_EM_FRACTION, freeResult, 0.001f)
        assertTrue(freeResult > 24f)

        val tableResult = PadGlyphRules.glyphSizeDp(24f, 80f, enlarge = true, fullBleed = false, isTableLayout = true)
        assertEquals(80f * PadGlyphRules.TABLE_ENLARGED_EM_FRACTION, tableResult, 0.001f)
        assertTrue(tableResult > 24f)
    }

    @Test
    fun glyphSizeDp_fullBleedActive_scalesToEdge() {
        val freeResult = PadGlyphRules.glyphSizeDp(24f, 80f, enlarge = false, fullBleed = true, isTableLayout = false)
        assertEquals(80f * PadGlyphRules.FREE_FULL_BLEED_EM_FRACTION, freeResult, 0.001f)
        assertTrue(freeResult > 80f * PadGlyphRules.FREE_ENLARGED_EM_FRACTION)

        val tableResult = PadGlyphRules.glyphSizeDp(24f, 80f, enlarge = false, fullBleed = true, isTableLayout = true)
        assertEquals(80f * PadGlyphRules.TABLE_FULL_BLEED_EM_FRACTION, tableResult, 0.001f)
        assertTrue(tableResult > 80f * PadGlyphRules.TABLE_ENLARGED_EM_FRACTION)
    }

    @Test
    fun defaultEnlargeIcon_tableLayout_returnsTrue() {
        assertTrue(PadGlyphRules.defaultEnlargeIcon(isTableLayout = true))
        assertFalse(PadGlyphRules.defaultEnlargeIcon(isTableLayout = false))
    }

    @Test
    fun showsEnlargeIcon_mutualExclusivity() {
        assertTrue(PadGlyphRules.showsEnlargeIcon(iconName = "settings", imageAssetId = null))
        assertFalse(PadGlyphRules.showsEnlargeIcon(iconName = "settings", imageAssetId = "img_123"))
        assertFalse(PadGlyphRules.showsEnlargeIcon(iconName = null, imageAssetId = null))
    }

    @Test
    fun showsEnlargeText_validations() {
        assertTrue(PadGlyphRules.showsEnlargeText(iconName = null, imageAssetId = null))
        assertTrue(PadGlyphRules.showsEnlargeText(iconName = null, imageAssetId = "img_123", showLabel = true))
        assertFalse(PadGlyphRules.showsEnlargeText(iconName = null, imageAssetId = "img_123", showLabel = false))
        assertFalse(PadGlyphRules.showsEnlargeText(iconName = "settings", imageAssetId = null))
    }
}

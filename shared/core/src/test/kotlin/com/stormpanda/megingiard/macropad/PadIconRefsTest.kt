package com.stormpanda.megingiard.macropad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PadIconRefsTest {
    private val defaultAction = PadAction.GamepadButton(0x130, "A")

    @Test
    fun `referencedImageAssetIds collects image IDs across profiles and layouts`() {
        val btn1 = PadButton(id = "b1", label = "A", posX = 0.5f, posY = 0.5f, action = defaultAction, imageAssetId = "img1")
        val btn2 = PadButton(id = "b2", label = "B", posX = 0.5f, posY = 0.5f, action = defaultAction, imageAssetId = "img2")
        val btn3 = PadButton(id = "b3", label = "C", posX = 0.5f, posY = 0.5f, action = defaultAction, imageAssetId = null)
        val btn4 = PadButton(id = "b4", label = "D", posX = 0.5f, posY = 0.5f, action = defaultAction, imageAssetId = "img1") // duplicate

        val layout1 = PadLayout(id = "l1", name = "L1", buttons = listOf(btn1, btn2))
        val layout2 = PadLayout(id = "l2", name = "L2", buttons = listOf(btn3, btn4))

        val profile1 = PadProfile(id = "p1", name = "P1", layouts = listOf(layout1))
        val profile2 = PadProfile(id = "p2", name = "P2", layouts = listOf(layout2))

        val result = referencedImageAssetIds(listOf(profile1, profile2))
        assertEquals(setOf("img1", "img2"), result)
    }

    @Test
    fun `referencedImageAssetIds ignores blank and null image IDs`() {
        val btn1 = PadButton(id = "b1", label = "A", posX = 0.5f, posY = 0.5f, action = defaultAction, imageAssetId = "")
        val btn2 = PadButton(id = "b2", label = "B", posX = 0.5f, posY = 0.5f, action = defaultAction, imageAssetId = null)
        val layout = PadLayout(id = "l1", name = "L1", buttons = listOf(btn1, btn2))
        val profile = PadProfile(id = "p1", name = "P1", layouts = listOf(layout))

        val result = referencedImageAssetIds(listOf(profile))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `PadGlyphRules computes size correctly`() {
        assertEquals(24f, PadGlyphRules.glyphSizeDp(defaultSizeDp = 24f, faceSizeDp = null, enlarge = false, fullBleed = false), 0.001f)
        assertEquals(80f * PadGlyphRules.ENLARGED_EM_FRACTION, PadGlyphRules.glyphSizeDp(defaultSizeDp = 24f, faceSizeDp = 100f, enlarge = true, fullBleed = false, reserveDp = 20f), 0.001f)
        assertEquals(100f * PadGlyphRules.FULL_BLEED_EM_FRACTION, PadGlyphRules.glyphSizeDp(defaultSizeDp = 24f, faceSizeDp = 100f, enlarge = false, fullBleed = true, reserveDp = 20f), 0.001f)
    }
}

package com.stormpanda.megingiard.macropad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PadIconRefsTest {
    private fun button(
        id: String,
        assetId: String? = null,
    ) = PadButton(
        id = id,
        label = id,
        posX = 0.5f,
        posY = 0.5f,
        action = PadAction.ScrollWheel,
        imageAssetId = assetId,
    )

    private fun profile(
        id: String,
        vararg layouts: PadLayout,
    ) = PadProfile(
        id = id,
        name = id,
        layouts = layouts.toList(),
        activeLayoutId = layouts.firstOrNull()?.id,
    )

    private fun layout(
        id: String,
        vararg buttons: PadButton,
    ) = PadLayout(id = id, name = id, buttons = buttons.toList())

    @Test
    fun testCollectsIdsAcrossProfiles() {
        val profiles =
            listOf(
                profile("p1", layout("l1", button("a", "aaa")), layout("l2", button("b", "bbb"))),
                profile("p2", layout("l3", button("c", "ccc"))),
            )
        assertEquals(setOf("aaa", "bbb", "ccc"), referencedImageAssetIds(profiles))
    }

    @Test
    fun testInactiveLayoutsStillCount() {
        val active = layout("active", button("a", "aaa"))
        val inactive = layout("inactive", button("b", "bbb"))
        val profiles = listOf(profile("p", active, inactive))
        assertTrue("bbb" in referencedImageAssetIds(profiles))
    }

    @Test
    fun testSharedIdsCollapseToOne() {
        val profiles =
            listOf(
                profile("p1", layout("l1", button("a", "same"), button("b", "same"))),
                profile("p2", layout("l2", button("c", "same"))),
            )
        assertEquals(setOf("same"), referencedImageAssetIds(profiles))
    }

    @Test
    fun testBlankOrMissingIgnored() {
        val profiles = listOf(profile("p", layout("l", button("a", ""), button("b", "  "), button("c"))))
        assertTrue(referencedImageAssetIds(profiles).isEmpty())
    }
}

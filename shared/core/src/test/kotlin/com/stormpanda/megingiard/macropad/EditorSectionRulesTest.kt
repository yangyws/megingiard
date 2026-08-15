package com.stormpanda.megingiard.macropad

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorSectionRulesTest {
    @Test
    fun testDefaultExpandedContainsAll() {
        assertEquals(setOf(EditorSection.PROFILE, EditorSection.LAYOUT), EditorSectionRules.DEFAULT_EXPANDED)
    }

    @Test
    fun testToggled() {
        val initial = EditorSectionRules.DEFAULT_EXPANDED
        val toggledProfile = EditorSectionRules.toggled(initial, EditorSection.PROFILE)
        assertEquals(setOf(EditorSection.LAYOUT), toggledProfile)

        val toggledBack = EditorSectionRules.toggled(toggledProfile, EditorSection.PROFILE)
        assertEquals(initial, toggledBack)
    }

    @Test
    fun testButtonListIndexOffset() {
        assertEquals(7, EditorSectionRules.buttonListIndexOffset(setOf(EditorSection.PROFILE, EditorSection.LAYOUT)))
        assertEquals(5, EditorSectionRules.buttonListIndexOffset(setOf(EditorSection.LAYOUT)))
        assertEquals(6, EditorSectionRules.buttonListIndexOffset(setOf(EditorSection.PROFILE)))
        assertEquals(4, EditorSectionRules.buttonListIndexOffset(emptySet()))
    }
}

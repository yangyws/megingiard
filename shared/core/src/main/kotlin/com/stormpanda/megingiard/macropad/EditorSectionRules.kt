package com.stormpanda.megingiard.macropad

/**
 * A collapsible section of the MacroPad editor body.
 *
 * Declaration order is render order, and both sections sit above the canvas.
 */
enum class EditorSection {
    /** Profile chips plus the macro-list entry that belongs to the profile. */
    PROFILE,

    /** Layout chips. The toolbar and the canvas below them are not part of the section. */
    LAYOUT,
}

/**
 * Which editor sections start open, and what collapsing them does to list indices.
 */
object EditorSectionRules {
    /**
     * Sections open the first time the editor is shown in a process.
     */
    val DEFAULT_EXPANDED: Set<EditorSection> = EditorSection.entries.toSet()

    /**
     * Items the editor body renders above the button list with every section expanded.
     *
     * In order: `section_profile`, `profiles`, `profile_toolbar`, `section_layout`,
     * `layouts`, `canvas`, `section_buttons`.
     */
    const val BUTTON_LIST_BASE_OFFSET: Int = 7

    /**
     * Items [EditorSection.PROFILE] removes when collapsed: `profiles` and
     * `profile_toolbar`. The section header itself always renders.
     */
    const val PROFILE_SECTION_ITEM_COUNT: Int = 2

    /**
     * Items [EditorSection.LAYOUT] removes when collapsed: `layouts`.
     */
    const val LAYOUT_SECTION_ITEM_COUNT: Int = 1

    /**
     * [expanded] with [section] flipped.
     */
    fun toggled(
        expanded: Set<EditorSection>,
        section: EditorSection,
    ): Set<EditorSection> = if (section in expanded) expanded - section else expanded + section

    /**
     * How many list items sit above the first button, given which sections are open.
     */
    fun buttonListIndexOffset(expanded: Set<EditorSection>): Int =
        BUTTON_LIST_BASE_OFFSET -
            (if (EditorSection.PROFILE in expanded) 0 else PROFILE_SECTION_ITEM_COUNT) -
            (if (EditorSection.LAYOUT in expanded) 0 else LAYOUT_SECTION_ITEM_COUNT)
}

package com.stormpanda.megingiard.macropad

import kotlin.math.max
import kotlin.math.min

/**
 * When a button's Material symbol is blown up to fill its face, and how big it gets.
 *
 * Pure functions with no Android dependency, so they are unit-testable without Robolectric.
 */
object PadGlyphRules {
    /**
     * Glyph em size as a fraction of the button face when [PadButton.enlargeIcon] is on in Free Mode.
     */
    const val FREE_ENLARGED_EM_FRACTION = 0.72f

    /**
     * Glyph em size as a fraction of the button face when [PadButton.fullBleedIcon] is on in Free Mode.
     * Scales glyph to visually fill the button face without clipping.
     */
    const val FREE_FULL_BLEED_EM_FRACTION = 0.95f

    /**
     * Glyph em size as a fraction of the button face when [PadButton.enlargeIcon] is on in Table Mode.
     */
    const val TABLE_ENLARGED_EM_FRACTION = 0.70f

    /**
     * Glyph em size as a fraction of the button face when [PadButton.fullBleedIcon] is on in Table Mode.
     */
    const val TABLE_FULL_BLEED_EM_FRACTION = 0.92f

    // Backward-compatibility aliases
    const val ENLARGED_EM_FRACTION = FREE_ENLARGED_EM_FRACTION
    const val FULL_BLEED_EM_FRACTION = FREE_FULL_BLEED_EM_FRACTION

    /**
     * Whether a new button created on a layout in [PadLayoutMode.GRID] starts enlarged.
     */
    fun defaultEnlargeIcon(isTableLayout: Boolean): Boolean = isTableLayout

    /**
     * Whether the edit dialog offers the enlarge icon / full bleed switches.
     */
    fun showsEnlargeIcon(
        iconName: String?,
        imageAssetId: String?,
    ): Boolean = imageAssetId == null && iconName != null

    /**
     * Whether the edit dialog offers the enlarge text switch.
     */
    fun showsEnlargeText(
        iconName: String?,
        imageAssetId: String?,
        showLabel: Boolean = true,
    ): Boolean = (iconName == null && imageAssetId == null) || (imageAssetId != null && showLabel)

    /**
     * Glyph size in dp for a face of [faceSizeDp] on its shorter side.
     */
    fun glyphSizeDp(
        defaultSizeDp: Float,
        faceSizeDp: Float?,
        enlarge: Boolean,
        fullBleed: Boolean = false,
        reserveDp: Float = 0f,
        isTableLayout: Boolean = false,
    ): Float {
        if (faceSizeDp == null || faceSizeDp <= 0f) return defaultSizeDp
        val fullBleedFraction = if (isTableLayout) TABLE_FULL_BLEED_EM_FRACTION else FREE_FULL_BLEED_EM_FRACTION
        val enlargedFraction = if (isTableLayout) TABLE_ENLARGED_EM_FRACTION else FREE_ENLARGED_EM_FRACTION

        if (fullBleed) {
            return max(defaultSizeDp, faceSizeDp * fullBleedFraction)
        }
        if (enlarge) {
            val available = max(0f, faceSizeDp - max(0f, reserveDp))
            return max(defaultSizeDp, min(available, faceSizeDp) * enlargedFraction)
        }
        return defaultSizeDp
    }
}

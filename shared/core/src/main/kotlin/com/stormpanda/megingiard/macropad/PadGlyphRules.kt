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
     * Glyph em size as a fraction of the button face when [PadButton.enlargeIcon] is on.
     */
    const val ENLARGED_EM_FRACTION = 0.76f

    /**
     * Glyph em size as a fraction of the button face when [PadButton.fullBleedIcon] is on.
     */
    const val FULL_BLEED_EM_FRACTION = 0.92f

    /**
     * Whether a new button created on a layout in [PadLayoutMode.GRID] starts enlarged.
     */
    fun defaultEnlargeIcon(isTableLayout: Boolean): Boolean = isTableLayout

    /**
     * Whether the edit dialog offers the enlarge switch at all.
     */
    fun showsEnlargeIcon(
        iconName: String?,
        imageAssetId: String?,
    ): Boolean = imageAssetId == null && iconName != null

    /**
     * Glyph size in dp for a face of [faceSizeDp] on its shorter side.
     */
    fun glyphSizeDp(
        defaultSizeDp: Float,
        faceSizeDp: Float?,
        enlarge: Boolean,
        fullBleed: Boolean = false,
        reserveDp: Float = 0f,
    ): Float {
        if (faceSizeDp == null || faceSizeDp <= 0f) return defaultSizeDp
        if (fullBleed) {
            return max(defaultSizeDp, faceSizeDp * FULL_BLEED_EM_FRACTION)
        }
        if (enlarge) {
            val available = max(0f, faceSizeDp - max(0f, reserveDp))
            return max(defaultSizeDp, min(available, faceSizeDp) * ENLARGED_EM_FRACTION)
        }
        return defaultSizeDp
    }
}

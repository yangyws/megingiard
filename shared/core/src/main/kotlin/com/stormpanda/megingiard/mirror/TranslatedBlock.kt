package com.stormpanda.megingiard.mirror

import kotlinx.serialization.Serializable

/**
 * Represents a localized translated text block on screen.
 * All coordinates are normalized [0.0, 1.0] relative to the source crop or destination cutout.
 */
@Serializable
data class TranslatedBlock(
    val normLeft: Float,
    val normTop: Float,
    val normRight: Float,
    val normBottom: Float,
    val fontHeightRatio: Float = 0.5f,
    val originalText: String,
    val translatedText: String,
)

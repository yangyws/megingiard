package com.stormpanda.megingiard.translation

import kotlinx.serialization.Serializable

/**
 * Represents a raw text block detected by an OCR engine.
 * Coordinates are normalized in range [0.0, 1.0] relative to the source image / cutout bounds.
 */
@Serializable
data class OcrDetectedBlock(
    val text: String,
    val normLeft: Float,
    val normTop: Float,
    val normRight: Float,
    val normBottom: Float,
    val confidence: Float = 1.0f,
    val fontHeightRatio: Float = 0.5f,
)

/**
 * Configuration options for image OCR pre-processing and bounding box filters.
 */
@Serializable
data class OcrProcessingOptions(
    val autoInvert: Boolean = true,
    val binarizeThreshold: Int = 128,
    val minConfidence: Float = 0.5f,
    val minBlockHeightRatio: Float = 0.02f,
    val maxBlockHeightRatio: Float = 0.80f,
)

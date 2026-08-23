package com.stormpanda.megingiard.translation

import com.stormpanda.megingiard.AppLog

private const val TAG = "CompositeOcrEngine"
private const val DEFAULT_CONFIDENCE = 0.95f
private const val DEFAULT_FONT_HEIGHT_RATIO = 0.5f

/**
 * Interface for pluggable OCR engines.
 */
interface OcrEngine {
    val name: String
    suspend fun recognize(
        pixels: IntArray,
        width: Int,
        height: Int,
        sourceLang: String = "auto",
    ): List<OcrDetectedBlock>
}

/**
 * Composite multi-tier OCR engine combining fast Tier A pixel font matching
 * with projection profile text bounding box extraction.
 */
class CompositeOcrEngine(
    private val mlKitEngine: MlKitOcrEngine = MlKitOcrEngine(),
) : OcrEngine {
    override val name: String = "CompositeOcrEngine"

    override suspend fun recognize(
        pixels: IntArray,
        width: Int,
        height: Int,
        sourceLang: String,
    ): List<OcrDetectedBlock> {
        if (width <= 0 || height <= 0 || pixels.isEmpty()) return emptyList()

        // 1. Try fast Tier A pixel font matching first
        val binary = ImageOcrPreprocessor.binarize(pixels, width, height)
        val regions = ImageOcrPreprocessor.segmentTextRegions(binary, width, height)

        val detectedBlocks = mutableListOf<OcrDetectedBlock>()

        for (region in regions) {
            val normLeft = region.left.toFloat() / width.toFloat()
            val normTop = region.top.toFloat() / height.toFloat()
            val normRight = region.right.toFloat() / width.toFloat()
            val normBottom = region.bottom.toFloat() / height.toFloat()

            val pixelFontText = PixelFontMatcher.matchRegion(binary, width, region)

            if (pixelFontText.isNotBlank()) {
                detectedBlocks.add(
                    OcrDetectedBlock(
                        text = pixelFontText,
                        normLeft = normLeft,
                        normTop = normTop,
                        normRight = normRight,
                        normBottom = normBottom,
                        confidence = DEFAULT_CONFIDENCE,
                        fontHeightRatio = DEFAULT_FONT_HEIGHT_RATIO,
                    )
                )
            }
        }

        if (detectedBlocks.isNotEmpty()) {
            AppLog.i(TAG, "Recognized ${detectedBlocks.size} Tier A pixel font block(s)")
            return detectedBlocks
        }

        // 2. Fall back to Tier B on-device Neural ML Kit OCR (supports Japanese, Chinese, English)
        AppLog.d(TAG, "No Tier A pixel font detected, falling back to ML Kit OCR")
        val mlKitBlocks = mlKitEngine.recognize(pixels, width, height, sourceLang)
        AppLog.i(TAG, "ML Kit recognized ${mlKitBlocks.size} block(s)")
        return mlKitBlocks
    }
}

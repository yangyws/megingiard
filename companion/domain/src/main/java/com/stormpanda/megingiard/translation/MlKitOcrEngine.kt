package com.stormpanda.megingiard.translation

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.stormpanda.megingiard.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "MlKitOcrEngine"
private const val DEFAULT_CONFIDENCE = 0.95f
private const val DEFAULT_FONT_HEIGHT_RATIO = 0.5f

/**
 * On-device neural OCR engine powered by Google ML Kit.
 * Handles Japanese, Chinese, and Latin/English game fonts with high accuracy.
 */
class MlKitOcrEngine : OcrEngine {
    override val name: String = "MlKitOcrEngine"

    private val japaneseRecognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }
    private val latinRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    private val chineseRecognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    override suspend fun recognize(
        pixels: IntArray,
        width: Int,
        height: Int,
        sourceLang: String,
    ): List<OcrDetectedBlock> = withContext(Dispatchers.Default) {
        if (width <= 0 || height <= 0 || pixels.isEmpty()) return@withContext emptyList()

        try {
            val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
            val image = InputImage.fromBitmap(bitmap, 0)

            val recognizer = when (sourceLang.lowercase()) {
                "ja", "japanese" -> japaneseRecognizer
                "zh", "chinese" -> chineseRecognizer
                "en", "english" -> latinRecognizer
                else -> japaneseRecognizer // Default to Japanese for retro / anime games
            }

            val visionText = Tasks.await(recognizer.process(image))
            bitmap.recycle()

            val detectedBlocks = mutableListOf<OcrDetectedBlock>()

            for (block in visionText.textBlocks) {
                if (block.lines.isEmpty()) {
                    val box = block.boundingBox
                    val text = block.text.trim()
                    if (text.isBlank()) continue

                    val normLeft = if (box != null) (box.left.toFloat() / width.toFloat()).coerceIn(0f, 1f) else 0.05f
                    val normTop = if (box != null) (box.top.toFloat() / height.toFloat()).coerceIn(0f, 1f) else 0.05f
                    val normRight = if (box != null) (box.right.toFloat() / width.toFloat()).coerceIn(normLeft + 0.01f, 1f) else 0.95f
                    val normBottom = if (box != null) (box.bottom.toFloat() / height.toFloat()).coerceIn(normTop + 0.01f, 1f) else 0.95f

                    val blockHeight = normBottom - normTop
                    val fontRatio = blockHeight.coerceIn(0.2f, 0.8f)

                    detectedBlocks.add(
                        OcrDetectedBlock(
                            text = text,
                            normLeft = normLeft,
                            normTop = normTop,
                            normRight = normRight,
                            normBottom = normBottom,
                            confidence = DEFAULT_CONFIDENCE,
                            fontHeightRatio = fontRatio,
                        )
                    )
                    AppLog.i(TAG, "ML Kit OCR recognized block: '$text' at ($normLeft, $normTop, $normRight, $normBottom)")
                } else {
                    for (line in block.lines) {
                        val lineBlocks = segmentLineIntoClusters(line, width, height)
                        detectedBlocks.addAll(lineBlocks)
                    }
                }
            }

            detectedBlocks
        } catch (e: Exception) {
            AppLog.e(TAG, "ML Kit OCR recognition failed", e)
            emptyList()
        }
    }

    private fun segmentLineIntoClusters(
        line: com.google.mlkit.vision.text.Text.Line,
        imageWidth: Int,
        imageHeight: Int,
    ): List<OcrDetectedBlock> {
        val elements = line.elements
        if (elements.isEmpty()) {
            return splitSpacedText(
                rawText = line.text,
                box = line.boundingBox,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
            )
        }

        // Cluster elements on the same line based on horizontal proximity
        val clusters = mutableListOf<MutableList<com.google.mlkit.vision.text.Text.Element>>()
        var currentCluster = mutableListOf<com.google.mlkit.vision.text.Text.Element>()

        for (el in elements) {
            if (el.text.isBlank()) continue

            if (currentCluster.isEmpty()) {
                currentCluster.add(el)
            } else {
                val prevEl = currentCluster.last()
                val prevBox = prevEl.boundingBox
                val currBox = el.boundingBox

                val shouldSplit = if (prevBox != null && currBox != null) {
                    val avgHeight = kotlin.math.max(1, (prevBox.height() + currBox.height()) / 2)
                    val horizontalGap = currBox.left - prevBox.right
                    // Split if gap is larger than 0.55x character height or larger than 16px
                    horizontalGap > kotlin.math.max(14, (avgHeight * 0.55f).toInt())
                } else {
                    false
                }

                if (shouldSplit) {
                    clusters.add(currentCluster)
                    currentCluster = mutableListOf(el)
                } else {
                    currentCluster.add(el)
                }
            }
        }
        if (currentCluster.isNotEmpty()) {
            clusters.add(currentCluster)
        }

        val result = mutableListOf<OcrDetectedBlock>()
        for (cluster in clusters) {
            var minLeft = Int.MAX_VALUE
            var minTop = Int.MAX_VALUE
            var maxRight = Int.MIN_VALUE
            var maxBottom = Int.MIN_VALUE

            for (el in cluster) {
                val box = el.boundingBox ?: continue
                if (box.left < minLeft) minLeft = box.left
                if (box.top < minTop) minTop = box.top
                if (box.right > maxRight) maxRight = box.right
                if (box.bottom > maxBottom) maxBottom = box.bottom
            }

            val clusterText = cluster.joinToString(separator = " ") { it.text.trim() }.trim()
            val clusterBox = if (minLeft < maxRight && minTop < maxBottom) {
                android.graphics.Rect(minLeft, minTop, maxRight, maxBottom)
            } else {
                line.boundingBox
            }

            val subBlocks = splitSpacedText(
                rawText = clusterText,
                box = clusterBox,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
            )
            result.addAll(subBlocks)
        }

        return result
    }

    private fun splitSpacedText(
        rawText: String,
        box: android.graphics.Rect?,
        imageWidth: Int,
        imageHeight: Int,
    ): List<OcrDetectedBlock> {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) return emptyList()

        // Check if text has multiple words separated by 2+ spaces, tabs, or full-width spaces
        val spaceRegex = Regex("[ \\t\\u3000]{2,}")
        val tokens = trimmed.split(spaceRegex).map { it.trim() }.filter { it.isNotBlank() }

        if (tokens.size <= 1 || box == null) {
            val normLeft = if (box != null) (box.left.toFloat() / imageWidth.toFloat()).coerceIn(0f, 1f) else 0.05f
            val normTop = if (box != null) (box.top.toFloat() / imageHeight.toFloat()).coerceIn(0f, 1f) else 0.05f
            val normRight = if (box != null) (box.right.toFloat() / imageWidth.toFloat()).coerceIn(normLeft + 0.01f, 1f) else 0.95f
            val normBottom = if (box != null) (box.bottom.toFloat() / imageHeight.toFloat()).coerceIn(normTop + 0.01f, 1f) else 0.95f
            val blockHeight = normBottom - normTop
            val fontRatio = blockHeight.coerceIn(0.2f, 0.8f)

            AppLog.i(TAG, "ML Kit OCR single block: '$trimmed' at ($normLeft, $normTop, $normRight, $normBottom)")
            return listOf(
                OcrDetectedBlock(
                    text = trimmed,
                    normLeft = normLeft,
                    normTop = normTop,
                    normRight = normRight,
                    normBottom = normBottom,
                    confidence = DEFAULT_CONFIDENCE,
                    fontHeightRatio = fontRatio,
                )
            )
        }

        // Subdivide bounding box proportionally across spaced tokens
        val totalLength = tokens.sumOf { it.length }
        val boxW = box.width().toFloat()
        var curX = box.left.toFloat()
        val blocks = mutableListOf<OcrDetectedBlock>()

        val normTop = (box.top.toFloat() / imageHeight.toFloat()).coerceIn(0f, 1f)
        val normBottom = (box.bottom.toFloat() / imageHeight.toFloat()).coerceIn(normTop + 0.01f, 1f)
        val fontRatio = (normBottom - normTop).coerceIn(0.2f, 0.8f)

        for (token in tokens) {
            val tokenW = (token.length.toFloat() / totalLength.toFloat()) * boxW
            val normLeft = (curX / imageWidth.toFloat()).coerceIn(0f, 1f)
            val normRight = ((curX + tokenW) / imageWidth.toFloat()).coerceIn(normLeft + 0.01f, 1f)

            blocks.add(
                OcrDetectedBlock(
                    text = token,
                    normLeft = normLeft,
                    normTop = normTop,
                    normRight = normRight,
                    normBottom = normBottom,
                    confidence = DEFAULT_CONFIDENCE,
                    fontHeightRatio = fontRatio,
                )
            )
            AppLog.i(TAG, "ML Kit OCR spaced token: '$token' at ($normLeft, $normTop, $normRight, $normBottom)")
            curX += tokenW
        }

        return blocks
    }
}

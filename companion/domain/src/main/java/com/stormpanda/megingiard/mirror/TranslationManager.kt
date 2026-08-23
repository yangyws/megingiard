package com.stormpanda.megingiard.mirror

import com.stormpanda.megingiard.AppLog
import com.stormpanda.megingiard.translation.CompositeOcrEngine
import com.stormpanda.megingiard.translation.CompositeTranslationEngine
import com.stormpanda.megingiard.translation.TranslationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "TranslationManager"
private const val DEFAULT_FONT_HEIGHT_RATIO = 0.5f

/**
 * State holder singleton managing in-place translated blocks for secondary display cutouts.
 */
object TranslationManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val engine = CompositeTranslationEngine()
    val ocrEngine = CompositeOcrEngine()

    private val _translatedBlocks = MutableStateFlow<Map<String, List<TranslatedBlock>>>(emptyMap())
    val translatedBlocks: StateFlow<Map<String, List<TranslatedBlock>>> = _translatedBlocks.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    fun updateTranslation(cutoutId: String, blocks: List<TranslatedBlock>) {
        AppLog.d(TAG, "updateTranslation for cutoutId=$cutoutId with ${blocks.size} block(s)")
        val current = HashMap(_translatedBlocks.value)
        current[cutoutId] = blocks
        _translatedBlocks.value = current
    }

    fun clearTranslation(cutoutId: String) {
        AppLog.d(TAG, "clearTranslation for cutoutId=$cutoutId")
        val current = HashMap(_translatedBlocks.value)
        current.remove(cutoutId)
        _translatedBlocks.value = current
    }

    fun clearAllTranslations() {
        AppLog.d(TAG, "clearAllTranslations")
        _translatedBlocks.value = emptyMap()
    }

    fun setTranslating(translating: Boolean) {
        AppLog.d(TAG, "setTranslating($translating)")
        _isTranslating.value = translating
    }

    /**
     * Translates a single text string through the active composite engine.
     */
    suspend fun translateText(
        text: String,
        sourceLang: String = "auto",
        targetLang: String = "zh-TW",
    ): TranslationResult {
        return engine.translate(text, sourceLang, targetLang)
    }

    /**
     * Processes an image pixel buffer through the OCR pipeline, translates detected blocks,
     * and updates the cutout overlay state.
     */
    suspend fun processOcrAndTranslate(
        cutoutId: String,
        pixels: IntArray,
        width: Int,
        height: Int,
        sourceLang: String = "auto",
    ): List<TranslatedBlock> {
        val detected = ocrEngine.recognize(pixels, width, height, sourceLang)
        if (detected.isEmpty()) return emptyList()

        val translated = mutableListOf<TranslatedBlock>()
        for (block in detected) {
            val res = engine.translate(block.text, sourceLang, "zh-TW")
            translated.add(
                TranslatedBlock(
                    normLeft = block.normLeft,
                    normTop = block.normTop,
                    normRight = block.normRight,
                    normBottom = block.normBottom,
                    fontHeightRatio = block.fontHeightRatio,
                    originalText = block.text,
                    translatedText = res.translatedText,
                )
            )
        }
        updateTranslation(cutoutId, translated)
        return translated
    }

    /**
     * Translates a sample or captured text block and places it in-place inside the target cutout.
     */
    fun translateAndDisplaySample(
        cutoutId: String,
        originalText: String = "勇者よ、旅立つ時が来た！",
        sourceLang: String = "ja",
    ) {
        scope.launch {
            _isTranslating.value = true
            try {
                AppLog.i(TAG, "Translating text for cutout=$cutoutId: '$originalText' (src=$sourceLang)")
                val result = engine.translate(originalText, sourceLang, "zh-TW")
                val block = TranslatedBlock(
                    normLeft = 0.05f,
                    normTop = 0.65f,
                    normRight = 0.95f,
                    normBottom = 0.95f,
                    fontHeightRatio = DEFAULT_FONT_HEIGHT_RATIO,
                    originalText = originalText,
                    translatedText = result.translatedText,
                )
                updateTranslation(cutoutId, listOf(block))
            } catch (e: Exception) {
                AppLog.e(TAG, "Failed translating text for cutout=$cutoutId: ${e.message}")
            } finally {
                _isTranslating.value = false
            }
        }
    }
}

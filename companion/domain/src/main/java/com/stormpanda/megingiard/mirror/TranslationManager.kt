package com.stormpanda.megingiard.mirror

import com.stormpanda.megingiard.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "TranslationManager"

/**
 * State holder singleton managing in-place translated blocks for secondary display cutouts.
 */
object TranslationManager {
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
}

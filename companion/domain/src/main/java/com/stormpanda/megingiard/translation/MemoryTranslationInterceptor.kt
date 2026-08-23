package com.stormpanda.megingiard.translation

import com.stormpanda.megingiard.AppLog
import com.stormpanda.megingiard.mirror.TranslatedBlock
import com.stormpanda.megingiard.mirror.TranslationManager
import com.stormpanda.megingiard.session.ActiveGameSession

private const val TAG = "MemoryInterceptor"
private const val DEFAULT_FONT_HEIGHT_RATIO = 0.5f

/**
 * Interceptor that directly extracts dialogue and text from active emulator memory (Tier 0).
 */
class MemoryTranslationInterceptor(
    private val memoryReader: MemoryReader = ProcMemReader(),
) {
    private var lastExtractedText: String? = null

    /**
     * Attempts to read game memory, decode text, translate, and update [TranslationManager].
     * Returns true if memory text was successfully intercepted and updated, false otherwise.
     */
    suspend fun interceptAndTranslate(
        cutoutId: String,
        session: ActiveGameSession,
        sourceLang: String = "ja",
    ): Boolean {
        val spec = EmulatorMemoryMap.findSpecByPackage(session.packageName)
            ?: EmulatorMemoryMap.findSpecBySystem(session.systemId)

        if (spec == null) {
            AppLog.d(TAG, "No emulator memory spec found for package='${session.packageName}' system='${session.systemId}'")
            return false
        }

        val hook = MemoryHookRegistry.findHook(session.systemId, session.gameTitle, session.titleId)
        if (hook == null) {
            AppLog.d(TAG, "No memory hook found for game='${session.gameTitle}' titleId='${session.titleId}'")
            return false
        }

        val pid = memoryReader.findPid(session.packageName)
        if (pid == null) {
            AppLog.w(TAG, "Failed finding PID for active emulator package='${session.packageName}'")
            return false
        }

        val rawBytes = memoryReader.readBytes(pid, hook.textAddressOffset, hook.maxReadLength)
        if (rawBytes == null || rawBytes.isEmpty()) {
            AppLog.w(TAG, "Failed reading memory bytes at 0x${hook.textAddressOffset.toString(16)}")
            return false
        }

        val decodedText = MemoryTextDecoder.decode(rawBytes, hook.encoding)
        if (decodedText.isBlank()) {
            AppLog.d(TAG, "Memory text buffer was blank or null-terminated")
            return false
        }

        if (decodedText == lastExtractedText) {
            AppLog.d(TAG, "Memory text unchanged, skipping re-translation")
            return true
        }

        AppLog.i(TAG, "Intercepted Tier 0 memory text: '$decodedText' for cutout=$cutoutId")
        lastExtractedText = decodedText

        val translationResult = TranslationManager.engine.translate(decodedText, sourceLang, "zh-TW")
        val block = TranslatedBlock(
            normLeft = hook.normLeft,
            normTop = hook.normTop,
            normRight = hook.normRight,
            normBottom = hook.normBottom,
            fontHeightRatio = DEFAULT_FONT_HEIGHT_RATIO,
            originalText = decodedText,
            translatedText = translationResult.translatedText,
        )

        TranslationManager.updateTranslation(cutoutId, listOf(block))
        return true
    }

    fun reset() {
        lastExtractedText = null
    }
}

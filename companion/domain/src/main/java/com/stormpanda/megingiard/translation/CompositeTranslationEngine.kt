package com.stormpanda.megingiard.translation

import com.stormpanda.megingiard.AppLog

private const val TAG = "CompositeTranslation"

/**
 * Multi-tier composite translation engine coordinating local caching,
 * zero-latency offline gaming glossaries, and online fallback engines.
 */
class CompositeTranslationEngine(
    val offlineTranslator: OfflineDictionaryTranslator = OfflineDictionaryTranslator(),
    val onlineTranslator: OnlineFallbackTranslator = OnlineFallbackTranslator(),
    val cache: TranslationCache = TranslationCache(),
    var isOnlineEnabled: Boolean = true,
) : TranslationEngine {
    override val name: String = "CompositeEngine"

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String,
    ): TranslationResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return TranslationResult(
                originalText = text,
                translatedText = "",
                sourceLang = sourceLang,
                targetLang = targetLang,
                engineName = name,
            )
        }

        // Tier 0: Memory Cache Check
        val cached = cache.get(trimmed, sourceLang, targetLang)
        if (cached != null) {
            return TranslationResult(
                originalText = text,
                translatedText = cached,
                sourceLang = sourceLang,
                targetLang = targetLang,
                engineName = "LocalCache",
                cached = true,
            )
        }

        // Tier 1: Offline Glossary & Tokenizer
        val offlineResult = offlineTranslator.translate(trimmed, sourceLang, targetLang)
        val hasUntranslatedKana = offlineResult != null && containsJapaneseKana(offlineResult.translatedText)

        if (offlineResult != null && !hasUntranslatedKana) {
            cache.put(trimmed, sourceLang, targetLang, offlineResult.translatedText)
            return offlineResult
        }

        // Tier 2: Online Fallback (if permitted and offline result is incomplete)
        if (isOnlineEnabled) {
            val onlineResult = onlineTranslator.translate(trimmed, sourceLang, targetLang)
            if (onlineResult != null && onlineResult.translatedText.isNotBlank()) {
                cache.put(trimmed, sourceLang, targetLang, onlineResult.translatedText)
                return onlineResult
            }
        }

        // Fallback to offline result if available
        if (offlineResult != null) {
            cache.put(trimmed, sourceLang, targetLang, offlineResult.translatedText)
            return offlineResult
        }

        // Tier 3: Best-effort ZhTw conversion fallback
        val bestEffort = ZhTwConverter.convertToTaiwanTraditional(trimmed)
        AppLog.d(TAG, "Fallback best effort conversion: '$trimmed' -> '$bestEffort'")
        val fallbackResult = TranslationResult(
            originalText = text,
            translatedText = bestEffort,
            sourceLang = sourceLang,
            targetLang = targetLang,
            engineName = "FallbackConverter",
        )
        cache.put(trimmed, sourceLang, targetLang, bestEffort)
        return fallbackResult
    }

    private fun containsJapaneseKana(s: String): Boolean {
        return s.any { c ->
            val code = c.code
            (code in 0x3040..0x309F) || (code in 0x30A0..0x30FF) || (code in 0xFF65..0xFF9F)
        }
    }
}

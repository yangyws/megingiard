package com.stormpanda.megingiard.translation

import com.stormpanda.megingiard.AppLog

private const val TAG = "OfflineDictTranslator"

/**
 * High-performance offline game dictionary and tokenized phrase replacement translator.
 * Operates with 0ms network latency and zero external dependencies.
 */
class OfflineDictionaryTranslator : TranslationEngine {
    override val name: String = "OfflineGlossary"

    private val COMMON_GRAMMAR_PARTICLES = listOf(
        "てください" to "請",
        "なさい" to "請",
        "ました" to "了",
        "でした" to "曾是",
        "ます" to "",
        "です" to "是",
        "した" to "了",
        "ない" to "未/不",
        "たい" to "想",
        "から" to "起/自",
        "まで" to "至",
        "より" to "比",
        "と" to "與",
        "で" to "在",
        "に" to "向",
        "へ" to "往",
        "を" to "",
        "は" to "",
        "が" to "",
        "の" to "的",
    )

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String,
    ): TranslationResult? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        // 1. Check exact match
        val exactMatch = GameGlossaryDictionary.findExactMatch(trimmed, sourceLang)
        if (exactMatch != null) {
            val converted = ZhTwConverter.convertToTaiwanTraditional(exactMatch.targetText)
            AppLog.d(TAG, "Exact glossary match: '$trimmed' -> '$converted'")
            return TranslationResult(
                originalText = text,
                translatedText = converted,
                sourceLang = sourceLang,
                targetLang = targetLang,
                engineName = name,
            )
        }

        // 2. Sliding window phrase matching & token replacement
        val resultBuilder = StringBuilder()
        var index = 0
        var matchedAny = false

        while (index < trimmed.length) {
            // Check glossary prefix match
            val glossaryMatch = GameGlossaryDictionary.findLongestMatch(trimmed, index, sourceLang)
            if (glossaryMatch != null) {
                resultBuilder.append(glossaryMatch.first.targetText)
                index += glossaryMatch.second
                matchedAny = true
                continue
            }

            // Check Japanese grammatical particles if source is Japanese or text contains Kana
            val isJapaneseContext = sourceLang.equals("ja", ignoreCase = true) ||
                (sourceLang.equals("auto", ignoreCase = true) && trimmed.any { c ->
                    val code = c.code
                    (code in 0x3040..0x309F) || (code in 0x30A0..0x30FF) || (code in 0xFF65..0xFF9F)
                })

            if (isJapaneseContext) {
                var particleMatched = false
                val sub = trimmed.substring(index)
                for ((particle, replacement) in COMMON_GRAMMAR_PARTICLES) {
                    if (sub.startsWith(particle)) {
                        resultBuilder.append(replacement)
                        index += particle.length
                        particleMatched = true
                        matchedAny = true
                        break
                    }
                }
                if (particleMatched) continue
            }

            // Fallback: append current char and advance
            resultBuilder.append(trimmed[index])
            index++
        }

        val translated = ZhTwConverter.convertToTaiwanTraditional(resultBuilder.toString())
        AppLog.d(TAG, "Tokenized dictionary translation: '$trimmed' -> '$translated' (matchedAny=$matchedAny)")

        return if (matchedAny || translated != trimmed) {
            TranslationResult(
                originalText = text,
                translatedText = translated,
                sourceLang = sourceLang,
                targetLang = targetLang,
                engineName = name,
            )
        } else {
            null
        }
    }
}

package com.stormpanda.megingiard.translation

/**
 * Interface for pluggable text translation engines (Offline Glossary, Machine Translation, Cloud Fallback).
 */
interface TranslationEngine {
    val name: String

    /**
     * Translates the given [text] from [sourceLang] to [targetLang].
     * Returns a [TranslationResult] or null if the engine cannot produce a translation.
     */
    suspend fun translate(
        text: String,
        sourceLang: String = "auto",
        targetLang: String = "zh-TW"
    ): TranslationResult?
}

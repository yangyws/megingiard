package com.stormpanda.megingiard.translation

import kotlinx.serialization.Serializable

/**
 * Supported source languages for real-time translation.
 */
@Serializable
enum class TranslationSourceLanguage(val code: String, val displayName: String) {
    AUTO("auto", "Auto Detect"),
    JAPANESE("ja", "日本語"),
    ENGLISH("en", "English");

    companion object {
        fun fromCode(code: String): TranslationSourceLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: AUTO
    }
}

/**
 * Supported target languages (Default: Traditional Chinese - Taiwan).
 */
@Serializable
enum class TranslationTargetLanguage(val code: String, val displayName: String) {
    ZH_TW("zh-TW", "繁體中文");

    companion object {
        fun fromCode(code: String): TranslationTargetLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ZH_TW
    }
}

/**
 * Entry in the gaming glossary dictionary.
 */
@Serializable
data class GlossaryEntry(
    val sourceText: String,
    val targetText: String,
    val category: String = "general",
    val isExactMatchOnly: Boolean = false,
)

/**
 * Result of translating a text segment.
 */
@Serializable
data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val engineName: String,
    val cached: Boolean = false,
)

package com.stormpanda.megingiard.translation

import com.stormpanda.megingiard.AppLog

private const val TAG = "JapaneseTokenizer"

/**
 * Character type category for Japanese text processing.
 */
enum class CharType {
    KANJI,
    HIRAGANA,
    KATAKANA,
    ASCII_OR_LATIN,
    DIGIT,
    PUNCTUATION,
    OTHER
}

/**
 * Represents a token in tokenized Japanese or general text.
 */
data class TextToken(
    val text: String,
    val type: CharType,
    val startIndex: Int,
    val endIndex: Int,
)

/**
 * Rule-based tokenizer for Japanese text segmentation and character classification.
 */
object JapaneseTokenizer {

    fun classifyChar(c: Char): CharType {
        val code = c.code
        return when {
            // Kanji: CJK Unified Ideographs (0x4E00 - 0x9FFF)
            code in 0x4E00..0x9FFF -> CharType.KANJI
            // Hiragana: (0x3040 - 0x309F)
            code in 0x3040..0x309F -> CharType.HIRAGANA
            // Katakana: (0x30A0 - 0x30FF) + Halfwidth Katakana (0xFF65 - 0xFF9F)
            code in 0x30A0..0x30FF || code in 0xFF65..0xFF9F -> CharType.KATAKANA
            // Digits (ASCII & Full-width)
            c.isDigit() || code in 0xFF10..0xFF19 -> CharType.DIGIT
            // ASCII / Latin
            (code in 'a'.code..'z'.code) || (code in 'A'.code..'Z'.code) || (code in 0xFF21..0xFF5A) -> CharType.ASCII_OR_LATIN
            // Punctuation (ASCII & CJK)
            c.isWhitespace() || code in 0x3000..0x303F || code in 0xFF01..0xFF0F || code in 0xFF1A..0xFF20 -> CharType.PUNCTUATION
            else -> CharType.OTHER
        }
    }

    /**
     * Tokenizes an input Japanese string into sequential character blocks grouped by script type.
     */
    fun tokenize(input: String): List<TextToken> {
        if (input.isEmpty()) return emptyList()

        val tokens = mutableListOf<TextToken>()
        var currentType = classifyChar(input[0])
        var startIndex = 0

        for (i in 1 until input.length) {
            val charType = classifyChar(input[i])
            if (charType != currentType) {
                tokens.add(
                    TextToken(
                        text = input.substring(startIndex, i),
                        type = currentType,
                        startIndex = startIndex,
                        endIndex = i,
                    )
                )
                currentType = charType
                startIndex = i
            }
        }

        tokens.add(
            TextToken(
                text = input.substring(startIndex),
                type = currentType,
                startIndex = startIndex,
                endIndex = input.length,
            )
        )

        AppLog.d(TAG, "tokenize '${input.take(20)}' -> ${tokens.size} tokens")
        return tokens
    }
}

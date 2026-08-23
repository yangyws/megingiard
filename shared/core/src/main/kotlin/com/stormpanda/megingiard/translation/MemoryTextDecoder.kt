package com.stormpanda.megingiard.translation

import java.nio.charset.Charset

/**
 * Utility for decoding raw byte buffers extracted from emulator process memory into clean game text strings.
 */
object MemoryTextDecoder {

    private val CHARSET_SJIS = Charset.forName("Shift_JIS")
    private val CHARSET_UTF8 = Charset.forName("UTF-8")
    private val CHARSET_UTF16LE = Charset.forName("UTF-16LE")
    private val CHARSET_ASCII = Charset.forName("US-ASCII")

    /**
     * Decodes a raw byte buffer according to the given [encoding].
     * Automatically truncates at the first null-terminator ('\0') and strips unprintable control codes.
     */
    fun decode(bytes: ByteArray, encoding: GameTextEncoding = GameTextEncoding.SHIFT_JIS): String {
        if (bytes.isEmpty()) return ""

        val nullIndex = bytes.indexOf(0)
        val validSlice = if (nullIndex >= 0) bytes.sliceArray(0 until nullIndex) else bytes
        if (validSlice.isEmpty()) return ""

        val charset = when (encoding) {
            GameTextEncoding.SHIFT_JIS -> CHARSET_SJIS
            GameTextEncoding.UTF_8 -> CHARSET_UTF8
            GameTextEncoding.UTF_16_LE -> CHARSET_UTF16LE
            GameTextEncoding.ASCII -> CHARSET_ASCII
        }

        val rawText = try {
            String(validSlice, charset)
        } catch (e: Exception) {
            String(validSlice, CHARSET_UTF8)
        }

        return sanitizeGameText(rawText)
    }

    /**
     * Removes game-specific control tags (e.g. "\c[1]", "<speed:2>") and non-printable control characters.
     */
    fun sanitizeGameText(raw: String): String {
        if (raw.isBlank()) return ""

        // Strip custom dialog tags like <...>, [...]
        val tagStripped = raw
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\[[A-Za-z0-9_]+\\]"), "")

        val sb = StringBuilder(tagStripped.length)
        for (c in tagStripped) {
            // Keep standard printable characters, full-width CJK characters, newlines, and spaces
            if (c == '\n' || c == '\r' || c == '\t' || c >= ' ') {
                sb.append(c)
            }
        }
        return sb.toString().trim()
    }
}

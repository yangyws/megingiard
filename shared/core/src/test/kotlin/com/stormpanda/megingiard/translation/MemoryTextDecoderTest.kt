package com.stormpanda.megingiard.translation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.Charset

class MemoryTextDecoderTest {

    @Test
    fun testDecodeShiftJisWithNullTerminator() {
        val original = "勇者よ、旅立つ時が来た！"
        val bytes = original.toByteArray(Charset.forName("Shift_JIS"))
        val bufferWithNull = bytes + byteArrayOf(0, 0x12, 0x34)

        val decoded = MemoryTextDecoder.decode(bufferWithNull, GameTextEncoding.SHIFT_JIS)
        assertEquals(original, decoded)
    }

    @Test
    fun testDecodeUtf8() {
        val original = "Opening the treasure chest!"
        val bytes = original.toByteArray(Charsets.UTF_8)
        val decoded = MemoryTextDecoder.decode(bytes, GameTextEncoding.UTF_8)
        assertEquals(original, decoded)
    }

    @Test
    fun testSanitizeGameTextTags() {
        val raw = "<color=red>警告！</color> [pause] 敵があらわれた！"
        val sanitized = MemoryTextDecoder.sanitizeGameText(raw)
        assertEquals("警告！  敵があらわれた！", sanitized)
    }

    @Test
    fun testEmptyOrZeroBytes() {
        val decoded = MemoryTextDecoder.decode(byteArrayOf(0, 0, 0))
        assertEquals("", decoded)
    }
}

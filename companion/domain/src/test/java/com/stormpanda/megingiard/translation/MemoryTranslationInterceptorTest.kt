package com.stormpanda.megingiard.translation

import com.stormpanda.megingiard.mirror.TranslationManager
import com.stormpanda.megingiard.session.ActiveGameSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class MemoryTranslationInterceptorTest {

    private class FakeMemoryReader(
        private val expectedPid: Int = 1234,
        private val rawBytes: ByteArray,
    ) : MemoryReader {
        override fun findPid(packageName: String): Int? = expectedPid
        override fun readBytes(pid: Int, address: Long, length: Int): ByteArray = rawBytes
    }

    @Test
    fun testInterceptAndTranslatePspSession() = runTest {
        val sampleText = "勇者よ、旅立つ時が来た！"
        val bytes = sampleText.toByteArray(Charset.forName("Shift_JIS")) + byteArrayOf(0)

        val fakeReader = FakeMemoryReader(rawBytes = bytes)
        val interceptor = MemoryTranslationInterceptor(memoryReader = fakeReader)

        val session = ActiveGameSession(
            packageName = "org.ppsspp.ppsspp",
            romPath = "/roms/psp/p3p.iso",
            gameTitle = "Persona 3 Portable",
            systemId = "psp",
            titleId = "ULJM05489",
        )

        val success = interceptor.interceptAndTranslate("cutout-1", session, "ja")
        assertTrue(success)

        val blocks = TranslationManager.translatedBlocks.value["cutout-1"]
        assertTrue(!blocks.isNullOrEmpty())
        assertEquals(sampleText, blocks?.first()?.originalText)
        assertEquals("勇者啊，踏上旅程的時刻到了！", blocks?.first()?.translatedText)
    }
}

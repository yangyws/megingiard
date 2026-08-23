package com.stormpanda.megingiard.translation

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OfflineDictionaryTranslatorTest {

    private val translator = OfflineDictionaryTranslator()

    @Test
    fun testExactMatchTranslation() = runTest {
        val result = translator.translate("ニューゲーム", "ja", "zh-TW")
        assertNotNull(result)
        assertEquals("新遊戲", result?.translatedText)
        assertEquals("OfflineGlossary", result?.engineName)
    }

    @Test
    fun testEnglishExactMatchTranslation() = runTest {
        val result = translator.translate("Load Game", "en", "zh-TW")
        assertNotNull(result)
        assertEquals("讀取進度", result?.translatedText)
    }

    @Test
    fun testPhraseAndGrammarTranslation() = runTest {
        val result = translator.translate("勇者よ、旅立つ時が来た！", "ja", "zh-TW")
        assertNotNull(result)
        assertEquals("勇者啊，踏上旅程的時刻到了！", result?.translatedText)
    }
}

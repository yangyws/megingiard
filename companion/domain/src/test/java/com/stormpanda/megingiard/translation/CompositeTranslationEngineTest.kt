package com.stormpanda.megingiard.translation

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositeTranslationEngineTest {

    @Test
    fun testOfflineHitAndCache() = runTest {
        val engine = CompositeTranslationEngine(isOnlineEnabled = false)
        val result1 = engine.translate("セーブ", "ja", "zh-TW")
        assertEquals("存檔", result1.translatedText)
        assertEquals("OfflineGlossary", result1.engineName)

        val result2 = engine.translate("セーブ", "ja", "zh-TW")
        assertEquals("存檔", result2.translatedText)
        assertTrue(result2.cached)
    }

    @Test
    fun testFallbackZhTwConversion() = runTest {
        val engine = CompositeTranslationEngine(isOnlineEnabled = false)
        val result = engine.translate("这是未知的测试文本", "auto", "zh-TW")
        assertEquals("這是未知的測試文本", result.translatedText)
    }
}

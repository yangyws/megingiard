package com.stormpanda.megingiard.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TranslationCacheTest {

    @Test
    fun testPutAndGet() {
        val cache = TranslationCache(maxEntries = 2)
        assertNull(cache.get("New Game", "en", "zh-TW"))

        cache.put("New Game", "en", "zh-TW", "新遊戲")
        assertEquals("新遊戲", cache.get("New Game", "en", "zh-TW"))
    }

    @Test
    fun testLruEviction() {
        val cache = TranslationCache(maxEntries = 2)
        cache.put("A", "en", "zh-TW", "甲")
        cache.put("B", "en", "zh-TW", "乙")
        cache.put("C", "en", "zh-TW", "丙")

        assertEquals(2, cache.size)
        assertNull(cache.get("A", "en", "zh-TW"))
        assertEquals("乙", cache.get("B", "en", "zh-TW"))
        assertEquals("丙", cache.get("C", "en", "zh-TW"))
    }
}

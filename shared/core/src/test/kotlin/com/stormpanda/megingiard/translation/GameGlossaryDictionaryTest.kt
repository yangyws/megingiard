package com.stormpanda.megingiard.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GameGlossaryDictionaryTest {

    @Test
    fun testExactJapaneseMatch() {
        val entry = GameGlossaryDictionary.findExactMatch("ニューゲーム", "ja")
        assertNotNull(entry)
        assertEquals("新遊戲", entry?.targetText)

        val saveEntry = GameGlossaryDictionary.findExactMatch("セーブ", "ja")
        assertNotNull(saveEntry)
        assertEquals("存檔", saveEntry?.targetText)
    }

    @Test
    fun testExactEnglishMatch() {
        val entry = GameGlossaryDictionary.findExactMatch("New Game", "en")
        assertNotNull(entry)
        assertEquals("新遊戲", entry?.targetText)

        val criticalHit = GameGlossaryDictionary.findExactMatch("Critical Hit", "en")
        assertNotNull(criticalHit)
        assertEquals("暴擊！", criticalHit?.targetText)
    }

    @Test
    fun testLongestPrefixMatch() {
        val text = "つづきからスタート"
        val match = GameGlossaryDictionary.findLongestMatch(text, 0, "ja")
        assertNotNull(match)
        assertEquals("つづきから", match?.first?.sourceText)
        assertEquals("繼續遊戲", match?.first?.targetText)
        assertEquals(5, match?.second)
    }

    @Test
    fun testNoMatchReturnsNull() {
        val entry = GameGlossaryDictionary.findExactMatch("未知のテキストxyz", "ja")
        assertNull(entry)
    }
}

package com.stormpanda.megingiard.translation

import org.junit.Assert.assertEquals
import org.junit.Test

class JapaneseTokenizerTest {

    @Test
    fun testTokenizeKanjiAndHiragana() {
        val input = "勇者よ、旅立つ時が来た！"
        val tokens = JapaneseTokenizer.tokenize(input)
        assertEquals(10, tokens.size)
        assertEquals("勇者", tokens[0].text)
        assertEquals(CharType.KANJI, tokens[0].type)
        assertEquals("よ", tokens[1].text)
        assertEquals(CharType.HIRAGANA, tokens[1].type)
    }

    @Test
    fun testTokenizeKatakana() {
        val input = "ニューゲーム"
        val tokens = JapaneseTokenizer.tokenize(input)
        assertEquals(1, tokens.size)
        assertEquals("ニューゲーム", tokens[0].text)
        assertEquals(CharType.KATAKANA, tokens[0].type)
    }

    @Test
    fun testTokenizeEmpty() {
        val tokens = JapaneseTokenizer.tokenize("")
        assertEquals(0, tokens.size)
    }
}

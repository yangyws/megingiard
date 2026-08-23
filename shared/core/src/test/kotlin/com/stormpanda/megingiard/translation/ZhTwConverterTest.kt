package com.stormpanda.megingiard.translation

import org.junit.Assert.assertEquals
import org.junit.Test

class ZhTwConverterTest {

    @Test
    fun testSimplifiedToTraditionalConversion() {
        val input = "游戏保存成功，当前生命值与魔法值已恢复！"
        val expected = "遊戲存檔成功，當前生命值與魔力已恢復！"
        val actual = ZhTwConverter.convertToTaiwanTraditional(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testGamingTerminologyStandardization() {
        val input = "主屏幕菜单设置与按键映射配置"
        val expected = "主螢幕選單設定與按鍵映射設定"
        val actual = ZhTwConverter.convertToTaiwanTraditional(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testBlankOrEmptyInput() {
        assertEquals("", ZhTwConverter.convertToTaiwanTraditional(""))
        assertEquals("   ", ZhTwConverter.convertToTaiwanTraditional("   "))
    }
}

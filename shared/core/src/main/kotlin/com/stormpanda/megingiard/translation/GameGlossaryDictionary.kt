package com.stormpanda.megingiard.translation

/**
 * Built-in High-Frequency Offline Gaming Glossary and Dialogue Dictionary.
 * Covers retro and modern RPG, Action, Strategy, and Menu terminology for Japanese and English to Traditional Chinese.
 */
object GameGlossaryDictionary {

    private val JAPANESE_GLOSSARY = listOf(
        // System & Menu
        GlossaryEntry("ニューゲーム", "新遊戲", "menu"),
        GlossaryEntry("はじめから", "從頭開始", "menu"),
        GlossaryEntry("つづきから", "繼續遊戲", "menu"),
        GlossaryEntry("ロード", "讀取進度", "menu"),
        GlossaryEntry("ロードする", "讀取進度", "menu"),
        GlossaryEntry("セーブ", "存檔", "menu"),
        GlossaryEntry("セーブする", "進行存檔", "menu"),
        GlossaryEntry("オプション", "選項設定", "menu"),
        GlossaryEntry("コンフィグ", "環境設定", "menu"),
        GlossaryEntry("設定", "設定", "menu"),
        GlossaryEntry("せってい", "設定", "menu"),
        GlossaryEntry("決定", "確認", "menu"),
        GlossaryEntry("けってい", "確認", "menu"),
        GlossaryEntry("キャンセル", "取消", "menu"),
        GlossaryEntry("もどる", "返回", "menu"),
        GlossaryEntry("戻る", "返回", "menu"),
        GlossaryEntry("おわり", "結束", "menu"),
        GlossaryEntry("終了", "結束", "menu"),
        GlossaryEntry("ポーズ", "暫停", "menu"),
        GlossaryEntry("ヘルプ", "說明", "menu"),
        GlossaryEntry("クレジット", "製作團隊", "menu"),

        // Battle & Commands
        GlossaryEntry("たたかう", "攻擊", "battle"),
        GlossaryEntry("戦う", "戰鬥", "battle"),
        GlossaryEntry("こうげき", "攻擊", "battle"),
        GlossaryEntry("攻撃", "攻擊", "battle"),
        GlossaryEntry("ぼうぎょ", "防禦", "battle"),
        GlossaryEntry("防御", "防禦", "battle"),
        GlossaryEntry("まほう", "魔法", "battle"),
        GlossaryEntry("魔法", "魔法", "battle"),
        GlossaryEntry("じゅもん", "咒文", "battle"),
        GlossaryEntry("呪文", "咒文", "battle"),
        GlossaryEntry("とくぎ", "特技", "battle"),
        GlossaryEntry("特技", "特技", "battle"),
        GlossaryEntry("スキル", "技能", "battle"),
        GlossaryEntry("アイテム", "道具", "battle"),
        GlossaryEntry("どうぐ", "道具", "battle"),
        GlossaryEntry("道具", "道具", "battle"),
        GlossaryEntry("そうび", "裝備", "battle"),
        GlossaryEntry("装備", "裝備", "battle"),
        GlossaryEntry("にげる", "逃跑", "battle"),
        GlossaryEntry("逃げる", "逃跑", "battle"),
        GlossaryEntry("たいれつ", "隊列", "battle"),
        GlossaryEntry("隊列", "隊列", "battle"),
        GlossaryEntry("作戦", "作戰", "battle"),
        GlossaryEntry("さくせん", "作戰", "battle"),
        GlossaryEntry("オート", "自動戰鬥", "battle"),
        GlossaryEntry("いれかえ", "替換", "battle"),

        // Stats & Attributes
        GlossaryEntry("ステータス", "狀態", "stats"),
        GlossaryEntry("つよさ", "能力值", "stats"),
        GlossaryEntry("強さ", "能力值", "stats"),
        GlossaryEntry("レベル", "等級", "stats"),
        GlossaryEntry("経験値", "經驗值", "stats"),
        GlossaryEntry("けいけんち", "經驗值", "stats"),
        GlossaryEntry("たいりょく", "體力", "stats"),
        GlossaryEntry("体力", "體力", "stats"),
        GlossaryEntry("まりょく", "魔力", "stats"),
        GlossaryEntry("魔力", "魔力", "stats"),
        GlossaryEntry("ちから", "力量", "stats"),
        GlossaryEntry("力", "力量", "stats"),
        GlossaryEntry("すばやさ", "速度", "stats"),
        GlossaryEntry("素早さ", "速度", "stats"),
        GlossaryEntry("みのまもり", "守備力", "stats"),
        GlossaryEntry("こうげきりょく", "攻擊力", "stats"),
        GlossaryEntry("攻撃力", "攻擊力", "stats"),
        GlossaryEntry("しゅびりょく", "防禦力", "stats"),
        GlossaryEntry("守備力", "防禦力", "stats"),
        GlossaryEntry("ぼうぎょりょく", "防禦力", "stats"),
        GlossaryEntry("防御力", "防禦力", "stats"),
        GlossaryEntry("うんのよさ", "幸運", "stats"),
        GlossaryEntry("ゴールド", "金幣", "stats"),
        GlossaryEntry("おかね", "金錢", "stats"),
        GlossaryEntry("お金", "金錢", "stats"),

        // Equipment Types
        GlossaryEntry("ぶき", "武器", "equipment"),
        GlossaryEntry("武器", "武器", "equipment"),
        GlossaryEntry("よろい", "鎧甲", "equipment"),
        GlossaryEntry("鎧", "鎧甲", "equipment"),
        GlossaryEntry("たて", "盾牌", "equipment"),
        GlossaryEntry("盾", "盾牌", "equipment"),
        GlossaryEntry("かぶと", "頭盔", "equipment"),
        GlossaryEntry("兜", "頭盔", "equipment"),
        GlossaryEntry("ぼうし", "帽子", "equipment"),
        GlossaryEntry("帽子", "帽子", "equipment"),
        GlossaryEntry("アクセサリ", "飾品", "equipment"),
        GlossaryEntry("装飾品", "飾品", "equipment"),

        // Common RPG Dialogues & Events
        GlossaryEntry("勇者よ、旅立つ時が来た！", "勇者啊，踏上旅程的時刻到了！", "dialogue"),
        GlossaryEntry("宝箱を開けた！", "打開了寶箱！", "dialogue"),
        GlossaryEntry("たからばこ を あけた！", "打開了寶箱！", "dialogue"),
        GlossaryEntry("かいしんの いちげき！", "會心一擊！", "dialogue"),
        GlossaryEntry("会心の一撃！", "會心一擊！", "dialogue"),
        GlossaryEntry("つうこんの いちげき！", "痛恨一擊！", "dialogue"),
        GlossaryEntry("痛恨の一撃！", "痛恨一擊！", "dialogue"),
        GlossaryEntry("ゲームオーバー", "遊戲結束", "dialogue"),
        GlossaryEntry("クエスト", "任務", "dialogue"),
        GlossaryEntry("ダンジョン", "地下城", "dialogue"),
        GlossaryEntry("ショップ", "商店", "dialogue"),
        GlossaryEntry("宿屋", "旅館", "dialogue"),
        GlossaryEntry("やどや", "旅館", "dialogue"),
        GlossaryEntry("教会", "教會", "dialogue"),
        GlossaryEntry("きょうかい", "教會", "dialogue"),
        GlossaryEntry("毒", "中毒", "status"),
        GlossaryEntry("どく", "中毒", "status"),
        GlossaryEntry("麻痺", "麻痺", "status"),
        GlossaryEntry("まひ", "麻痺", "status"),
        GlossaryEntry("眠り", "睡眠", "status"),
        GlossaryEntry("ねむり", "睡眠", "status"),
        GlossaryEntry("混乱", "混亂", "status"),
        GlossaryEntry("こんらん", "混亂", "status"),
        GlossaryEntry("死亡", "陣亡", "status"),
        GlossaryEntry("しぼう", "陣亡", "status"),
    )

    private val ENGLISH_GLOSSARY = listOf(
        GlossaryEntry("New Game", "新遊戲", "menu"),
        GlossaryEntry("Continue", "繼續遊戲", "menu"),
        GlossaryEntry("Load Game", "讀取進度", "menu"),
        GlossaryEntry("Save Game", "儲存進度", "menu"),
        GlossaryEntry("Options", "選項設定", "menu"),
        GlossaryEntry("Settings", "系統設定", "menu"),
        GlossaryEntry("Configuration", "環境設定", "menu"),
        GlossaryEntry("Audio", "音效設定", "menu"),
        GlossaryEntry("Video", "影像設定", "menu"),
        GlossaryEntry("Controls", "操作控制", "menu"),
        GlossaryEntry("Keybindings", "按鍵綁定", "menu"),
        GlossaryEntry("Confirm", "確認", "menu"),
        GlossaryEntry("Cancel", "取消", "menu"),
        GlossaryEntry("Back", "返回", "menu"),
        GlossaryEntry("Exit", "離開", "menu"),
        GlossaryEntry("Quit", "結束遊戲", "menu"),
        GlossaryEntry("Pause", "暫停", "menu"),
        GlossaryEntry("Resume", "繼續", "menu"),
        GlossaryEntry("Retry", "重試", "menu"),
        GlossaryEntry("Restart", "重新開始", "menu"),

        // Battle & RPG
        GlossaryEntry("Attack", "攻擊", "battle"),
        GlossaryEntry("Defend", "防禦", "battle"),
        GlossaryEntry("Defense", "防禦", "battle"),
        GlossaryEntry("Guard", "格擋", "battle"),
        GlossaryEntry("Magic", "魔法", "battle"),
        GlossaryEntry("Spell", "法術", "battle"),
        GlossaryEntry("Skill", "技能", "battle"),
        GlossaryEntry("Special", "特技", "battle"),
        GlossaryEntry("Item", "道具", "battle"),
        GlossaryEntry("Items", "道具欄", "battle"),
        GlossaryEntry("Equipment", "裝備", "battle"),
        GlossaryEntry("Equip", "裝備", "battle"),
        GlossaryEntry("Unequip", "卸下", "battle"),
        GlossaryEntry("Status", "狀態", "stats"),
        GlossaryEntry("Escape", "逃跑", "battle"),
        GlossaryEntry("Flee", "逃跑", "battle"),
        GlossaryEntry("Run", "逃跑", "battle"),
        GlossaryEntry("Inventory", "物品清單", "battle"),
        GlossaryEntry("Quest", "任務", "stats"),
        GlossaryEntry("Quests", "任務清單", "stats"),
        GlossaryEntry("Level", "等級", "stats"),
        GlossaryEntry("Experience", "經驗值", "stats"),
        GlossaryEntry("Health", "生命值", "stats"),
        GlossaryEntry("Mana", "魔力", "stats"),
        GlossaryEntry("Stamina", "耐力", "stats"),
        GlossaryEntry("Strength", "力量", "stats"),
        GlossaryEntry("Agility", "敏捷", "stats"),
        GlossaryEntry("Intelligence", "智力", "stats"),
        GlossaryEntry("Critical Hit", "暴擊！", "battle"),
        GlossaryEntry("Game Over", "遊戲結束", "dialogue"),
        GlossaryEntry("Victory", "勝利", "dialogue"),
        GlossaryEntry("Defeat", "戰敗", "dialogue"),
    )

    private val customEntries = mutableListOf<GlossaryEntry>()

    /**
     * Find an exact match for the given text.
     */
    fun findExactMatch(text: String, sourceLang: String = "auto"): GlossaryEntry? {
        val trimmed = text.trim()
        val custom = customEntries.firstOrNull { it.sourceText.equals(trimmed, ignoreCase = true) }
        if (custom != null) return custom

        return when (sourceLang.lowercase()) {
            "ja" -> JAPANESE_GLOSSARY.firstOrNull { it.sourceText.equals(trimmed, ignoreCase = true) }
            "en" -> ENGLISH_GLOSSARY.firstOrNull { it.sourceText.equals(trimmed, ignoreCase = true) }
            else -> {
                JAPANESE_GLOSSARY.firstOrNull { it.sourceText.equals(trimmed, ignoreCase = true) }
                    ?: ENGLISH_GLOSSARY.firstOrNull { it.sourceText.equals(trimmed, ignoreCase = true) }
            }
        }
    }

    /**
     * Search for the longest matching glossary term starting at index [startIndex] in [text].
     */
    fun findLongestMatch(text: String, startIndex: Int, sourceLang: String = "auto"): Pair<GlossaryEntry, Int>? {
        if (startIndex >= text.length) return null
        val sub = text.substring(startIndex)

        val list = when (sourceLang.lowercase()) {
            "ja" -> JAPANESE_GLOSSARY + customEntries
            "en" -> ENGLISH_GLOSSARY + customEntries
            else -> JAPANESE_GLOSSARY + ENGLISH_GLOSSARY + customEntries
        }

        var longestMatch: Pair<GlossaryEntry, Int>? = null
        for (entry in list) {
            val src = entry.sourceText
            val match = if (sourceLang.lowercase() == "en") {
                sub.startsWith(src, ignoreCase = true)
            } else {
                sub.startsWith(src)
            }

            if (match) {
                if (longestMatch == null || src.length > longestMatch.second) {
                    longestMatch = Pair(entry, src.length)
                }
            }
        }
        return longestMatch
    }

    fun addCustomGlossary(entries: List<GlossaryEntry>) {
        customEntries.addAll(entries)
    }

    fun clearCustomGlossary() {
        customEntries.clear()
    }
}

# 專案變更日誌與追溯索引 (Changelog & Traceability Index)

本檔案遵循全域技能規範，記錄專案重要修改歷史與技術決策索引。每次修改皆給定唯一索引識別碼，以利後續追溯與維護。

---

## 🔖 [MOD-20260920-26] 補齊未翻譯英文與台灣繁體中文在地化用語全方位純淨化

* **修改日期**：2026-09-20
* **目標分支**：`feature/traditional-chinese-localization`
* **修改分類**：`[在地化精修 / 台灣繁體中文標準 / 詞彙純淨化]`
* **涉及檔案清單**：
  * 修改：`companion/ui/src/main/res/values-zh-rTW/strings.xml`（補齊 3 處未翻譯英文，純淨化 10 處非台灣用語）
  * 新增：`CHANGELOG.md`（建立專案變更日誌與索引追溯紀錄）
* **修改動機與問題**（Why）：
  * 深度稽核發現 `companion` 模組在繁體中文資源中仍遺留部分未翻譯英文文字（如 `macropad_no_profile`、`macropad_action_group_app_launcher`、`mirror_editor_saved_badge`）。
  * 同時排查出多處中國大陸習慣用語與非台灣風格詞彙（如「日誌級別」、「日誌報告」、「系統日誌」、「最近任務列表」、「獲取幫助」、「支持此應用程式」等），未達台灣 Android 官方標準。
* **技術方案與關鍵決策**（How）：
  1. **補齊未翻譯英文字串**：
     - `macropad_no_profile` $\rightarrow$ 「尚未設定設定檔。\n請開啟「設定」以建立版面配置。」
     - `macropad_action_group_app_launcher` $\rightarrow$ 「應用程式快速切換」
     - `mirror_editor_saved_badge` $\rightarrow$ 「已儲存」
  2. **全面用語純淨化轉換**：
     - `日誌` $\rightarrow$ `記錄檔`（記錄檔層級、儲存記錄檔報告、系統記錄檔）
     - `列表` $\rightarrow$ `清單`（最近使用的應用程式清單）
     - `最近任務` $\rightarrow$ `最近使用的應用程式`
     - `獲取幫助` $\rightarrow$ `取得說明`
     - `支持此應用程式` $\rightarrow$ `贊助此應用程式`
* **測試與驗證結果**（Verification）：
  * XML 語法驗證通過（1,017 條字串完全合法解析）。
  * 稽核工具驗證所有人類可讀英文字串 100% 翻譯完畢，大陸習慣用語命中徹底歸零。

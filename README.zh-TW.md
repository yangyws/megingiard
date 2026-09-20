# Megingiard 掌機雙螢幕工具箱 (AYN Thor 專屬)

[English](README.md) | **台灣繁體中文**

---

歡迎使用 **Megingiard** —— 專為 **AYN Thor** 雙螢幕 Android 遊戲掌機量身打造的強大隨身工具箱應用程式。Megingiard 將 Android 底層硬體視訊串流處理技術與現代 Jetpack Compose 介面深度整合，將掌機的副螢幕轉化為功能齊全的互動工具台：包含零延遲、多區域裁切的主螢幕鏡像、虛擬鍵盤、虛擬觸控板、可自訂之巨集快捷板 (MacroPad) 以及虛擬遊戲手把 —— 全數透過原生輸入注入驅動，達成亞毫秒級 (sub-millisecond) 的極速反應。

<p align="center">
  <a href="https://youtu.be/vgs6X9piswA?si=K8TbTrWHGzLIRxe3">
    <img src="https://img.youtube.com/vi/vgs6X9piswA/hqdefault.jpg" alt="Megingiard 功能概覽 (Rye J's Outpost)" width="390">
  </a>
  <a href="https://youtu.be/1Iksugqljj8">
    <img src="https://img.youtube.com/vi/1Iksugqljj8/hqdefault.jpg" alt="將所有遊戲轉化為雙螢幕遊戲 (RoeTaKa)" width="390">
  </a>
  <br>
  <em>功能介紹影片由 <a href="https://youtu.be/vgs6X9piswA?si=K8TbTrWHGzLIRxe3">Rye J’s Outpost</a> &nbsp;·&nbsp; 雙螢幕遊戲體驗介紹由 <a href="https://youtu.be/1Iksugqljj8">RoeTaKa</a> 提供</em>
</p>

---

[裝置相容性](#裝置相容性) · [核心特色](#核心特色) · [安裝方式](#安裝方式) · [初次啟動與快速上手](#初次啟動與快速上手) · [特權模式-privileged-mode](#特權模式-privileged-mode) · [隱私權說明](#隱私權說明) · [常見問題與疑難排解](#常見問題與疑難排解) · [授權條款](#授權條款)

---

## 裝置相容性

- **目標裝置：** AYN Thor（具備雙螢幕的 Android 旗艦遊戲掌機）
- **最低 Android 版本需求：** Android 13 / API 33（AYN Thor 出廠預設版本）
- **主要顯示器：** 6 吋 AMOLED (1920×1080)
- **次要顯示器（副螢幕）：** 3.92 吋 AMOLED (1080×1240)
- **架構：** 64 位元 ARM (arm64-v8a)

---

## 核心特色

### 🪞 零延遲主螢幕鏡像 (Screen Mirroring)
- **即時硬體串流**：以最高 60 FPS 幀率與亞訊框 (sub-frame) 延遲將主螢幕即時鏡像至副螢幕。
- **多區域獨立裁切**：支援同時設定多達 4 組不同的主螢幕裁切區塊，並各自設定縮放比例與位置（適合 DS/3DS 雙螢幕模擬、小地圖常駐顯示、HUD 狀態面板等）。
- **觸控雙向回傳**：直接點擊副螢幕即可將觸控操作即時映射回主螢幕對應座標。

### ⌨️ 虛擬輸入工具
- **虛擬鍵盤**：全尺寸副螢幕虛擬鍵盤，支援實體手把方向鍵導航與觸控雙輸入。
- **虛擬觸控板**：將副螢幕化身為高精度滑鼠觸控板，支援單指移動、雙指捲動與多點觸控手勢。
- **自訂巨集板 (MacroPad)**：自由配置快捷按鍵面板，一鍵執行複合按鍵、快速啟動應用程式、傳送字串或切換設定設定檔。
- **虛擬手把**：副螢幕即時觸控手把配置，支援完全自訂鍵位映射。

### 🇹🇼 100% 原生台灣繁體中文支援
- 全介面採用 Android 官方標準資源在地化，嚴格遵循台灣科技與遊戲術語（手把、觸控板、設定檔、記錄檔、快捷鍵、螢幕等）。
- 原生支援 Android 13+「各應用程式語言設定」，熱切換即時生效。

---

## 安裝方式

1. 至本儲存庫的 **[Releases 最新發布頁面](https://github.com/yangyws/megingiard/releases)** 下載最新的 APK 檔案。
2. 將 APK 傳輸至您的 AYN Thor 掌機，或使用 ADB 進行安裝：
   ```pwsh
   adb install -r Megingiard.apk
   ```

---

## 初次啟動與快速上手

1. 在 AYN Thor 上開啟 **Megingiard**。
2. 依據系統提示授予必要的「螢幕擷取」與「無障礙服務」權限。
3. 若需更順暢且無權限對話框的體驗，建議啟用 **Shizuku 特權模式**。

---

## 特權模式 (Privileged Mode)

Megingiard 支援透過 **[Shizuku](https://shizuku.rikka.app/)** 取得系統 ADB 級權限：
- 免去每次重開機後頻繁彈出的系統錄影/擷取確認對話框。
- 實現更低延遲的原生輸入注入機制。

---

## 授權條款

Megingiard 採用 **GNU General Public License, version 3 (GPLv3)** 授權條款釋出。詳細資訊請參閱本儲存庫之 `LICENSE` 檔案。

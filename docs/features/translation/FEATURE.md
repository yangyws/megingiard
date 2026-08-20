# Feature: In-Place Game Translation Pipeline (即時遊戲翻譯)

> **Status:** In Progress (Phase 1 Implemented)  
> **Target Device:** AYN Thor dual-screen Android handheld  
> **Source Languages:** Japanese (`ja`), English (`en`), Auto Detect  
> **Target Language:** Traditional Chinese (`zh-TW`)  

---

## 1. Feature Overview

The In-Place Game Translation Pipeline enables players on the AYN Thor handheld to read untranslated foreign-language retro and modern games (Japanese and English) in real time on the secondary display (Display 4).

### Key Highlights
1. **In-Place Visual Replacement**: Instead of displaying text in a detached floating box, Megingiard renders a subtle semi-transparent dark mask over the original text box on the mirrored screen, then draws clear Traditional Chinese text with bold black stroke and pure white fill directly in-place.
2. **Tap-to-Translate UX**: The user simply taps the mirrored cutout area on the secondary touch screen:
   - **1st Tap**: Reads text -> Translates -> Displays the in-place translated text overlay.
   - **2nd Tap**: Dismisses the overlay and returns to the clean original feed.
3. **Multi-Tier Fallback Strategy (A 失敗走 B，B 失敗走 C)**:
   - **Tier 0 (Highest Priority - Memory Read)**: Reads emulator/game memory directly via `megingiard_privd` (0ms latency, 100% accuracy, zero OCR noise).
   - **Tier A (Pixel-Font Matcher)**: Bitmask / glyph matcher for retro 8-bit/16-bit games (<15ms).
   - **Tier B (On-Device Vision Transformer)**: Manga-OCR via ONNX Runtime Android (30~50ms offline for complex Japanese fonts).
   - **Tier C (General On-Device OCR)**: Google ML Kit OCR with 4x nearest-neighbor binarization and thresholding for English and standard text.
   - **Tier D (Cloud Multimodal Fallback)**: Gemini 1.5 Flash vision fallback when cloud translation is permitted.
4. **Source Language Selection**:
   - Japanese (`ja`)
   - English (`en`)
   - Auto Detect (`auto`)
   - Target is always **Traditional Chinese (`zh-TW`)** with Taiwan gaming terminology dictionaries.

---

## 2. Technical Architecture & Modules

### 2.1 Core Models (`:shared:core`)
- `CutoutMode`: `MIRROR`, `TOUCH_PROJECTION`, `TRANSLATION`, `BOTH`.
- `ScreenCutout`:
  - `cutoutMode: CutoutMode`
  - `isTranslationEnabled: Boolean`
  - `targetTranslationCutoutId: String?`
  - `sourceLanguage: String` (e.g. `"ja"`, `"en"`, `"auto"`)
- `TranslatedBlock`:
  - `normLeft`, `normTop`, `normRight`, `normBottom` (bounding box normalized to [0.0, 1.0])
  - `fontHeightRatio: Float`
  - `originalText: String`
  - `translatedText: String`

### 2.2 Domain State Management (`:companion:domain`)
- `TranslationManager`:
  - `_translatedBlocks: MutableStateFlow<Map<String, List<TranslatedBlock>>>`
  - `updateTranslation(cutoutId, blocks)`
  - `clearTranslation(cutoutId)`
  - `clearAllTranslations()`
- `TouchProjectionController`:
  - Detects touch taps inside cutouts with `isTranslationEnabled` and invokes `onTranslationCutoutTapped(cutoutId)`.

### 2.3 UI & Presentation (`:companion:ui`)
- `MirrorPresentation` & `MultiCutoutContainer`:
  - Observes `TranslationManager.translatedBlocks`.
  - In `dispatchDraw`, renders `transBgPaint` (semi-transparent dark rounded rect) and calls `drawAutoSizedText` with `transStrokePaint` (black outline) + `transTextPaint` (white fill) using Android `StaticLayout`.
- `BackgroundSettingsOverlay`:
  - Settings toggle switch for each cutout: "即時翻譯 (Live Translation)".
  - Source language dropdown (日文 / 英文 / 自動).

---

## 3. Implementation Roadmap

- [x] **Phase 1: Data Model, In-Place Canvas Rendering & Tap-to-Translate UX** (Completed on 2026-08-21, commit `799d6126`).
- [ ] **Phase 2: Offline Dictionary & Tokenization Translation Pipeline**:
  - Japanese tokenization (`JapaneseTokenizer`), English tokenization.
  - Offline bilingual dictionaries (`DictionaryLookup`) + `ZhTwConverter`.
- [ ] **Phase 3: Tier 0 Memory Text Interceptor**:
  - Hook emulator memory (Citra/Lime3DS, Cemu, PPSSPP, RetroArch) via `megingiard_privd`.
- [ ] **Phase 4: Tier A/B/C Multi-Stage Offline OCR Pipeline**:
  - Pixel font matcher (Tier A) + Manga-OCR ONNX (Tier B) + ML Kit (Tier C).
- [ ] **Phase 5: MacroPad & QuickMenu Shortcuts**:
  - Quick action buttons: Mirror / Translate / Screenshot.

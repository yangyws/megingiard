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

### 2.4 Translation Engines & Glossary Pipeline (`:shared:core` & `:companion:domain`)
- `ZhTwConverter`: Converts Simplified Chinese characters and standardizes Taiwan gaming terminology (e.g. `選單`, `存檔`, `魔力`, `暴擊`, `道具欄`, `手把`, `搖桿`).
- `GameGlossaryDictionary`: High-frequency retro and modern RPG/Action gaming vocabulary covering Japanese and English terms.
- `JapaneseTokenizer`: Character classification (Kanji, Hiragana, Katakana, digits, punctuation) and script tokenization.
- `TranslationCache`: Thread-safe LRU cache storing historical translations for instant offline retrieval.
- `CompositeTranslationEngine`: Orchestrates local cache -> zero-latency offline glossary & tokenized grammar matcher -> online REST fallback -> Taiwanese Traditional Chinese standardizer.

### 2.5 Tier 0 Emulator Memory Interceptor (`:shared:core` & `:companion:domain`)
- `EmulatorMemoryMap`: Memory specifications for PPSSPP (PSP RAM), Citra/Lime3DS (FCRAM), RetroArch (GBA EWRAM), AetherSX2/NetherSX2 (PS2 EE RAM), and Dolphin (GC/Wii MEM1).
- `MemoryTextDecoder`: Raw byte decoder supporting Shift-JIS, UTF-8, and UTF-16LE with null-terminator slicing, binary garbage filtering, and control tag stripping.
- `MemoryReader` & `ProcMemReader`: Privileged `/proc/[pid]/mem` random-access reader.
- `MemoryDialogueHook` & `MemoryHookRegistry`: Pre-configured and custom game memory dialogue hooks.
- `MemoryTranslationInterceptor`: Intercepts active game sessions, reads raw memory buffers, decodes, and triggers instant translation.

### 2.6 Tier A/B/C Multi-Stage OCR Pipeline (`:shared:core` & `:companion:domain`)
- `OcrDetectedBlock` & `OcrProcessingOptions`: Normalized OCR bounding boxes and filtering parameters.
- `PixelFontGlyphMap`: 8x8 and 16x16 classic bitmask signatures for retro pixel fonts.
- `ImageOcrPreprocessor`: Pure pixel buffer luminance calculation, automatic dark/light background inversion, adaptive binarization, and horizontal/vertical projection profile line segmenter.
- `PixelFontMatcher`: <15ms Tier A retro bitmap glyph matcher.
- `CompositeOcrEngine`: Orchestrates image preprocessing -> Tier A pixel matcher -> projection text line segmenter -> normalized OCR output.
- `TranslationManager.processOcrAndTranslate`: Full pipeline connecting bitmap/pixel buffer extraction -> multi-stage OCR -> translation engine -> in-place visual overlay update.

---

## 3. Implementation Roadmap

- [x] **Phase 1: Data Model, In-Place Canvas Rendering & Tap-to-Translate UX** (Completed on 2026-08-21, commit `799d6126`).
- [x] **Phase 2: Offline Dictionary & Tokenization Translation Pipeline** (Completed on 2026-08-22):
  - Japanese tokenization (`JapaneseTokenizer`), English phrase matching.
  - Offline bilingual dictionaries (`GameGlossaryDictionary`) + `ZhTwConverter` gaming standardizer.
  - Multi-tier composite translation coordinator with LRU memory caching (`CompositeTranslationEngine`, `TranslationCache`).
- [x] **Phase 3: Tier 0 Memory Text Interceptor** (Completed on 2026-08-22):
  - Hook emulator memory (PPSSPP, Citra/Lime3DS, RetroArch, AetherSX2, Dolphin) via `EmulatorMemoryMap` and `MemoryReader`.
  - Binary text decoders (`MemoryTextDecoder`) for Shift-JIS, UTF-8, and UTF-16LE.
  - Active game session memory dialogue hook interception (`MemoryTranslationInterceptor`).
- [x] **Phase 4: Tier A/B/C Multi-Stage Offline OCR Pipeline** (Completed on 2026-08-22):
  - Pixel font matcher (`PixelFontMatcher` Tier A) + bitmask signature dictionary (`PixelFontGlyphMap`).
  - Image preprocessing and projection profile text segmenter (`ImageOcrPreprocessor`).
  - Multi-stage coordinator (`CompositeOcrEngine`) integrated with `TranslationManager`.
- [ ] **Phase 5: MacroPad & QuickMenu Shortcuts**:
  - Quick action buttons: Mirror / Translate / Screenshot.

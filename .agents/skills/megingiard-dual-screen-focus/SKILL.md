---
name: megingiard-dual-screen-focus
description: "Audit, implement, and maintain dual-screen gamepad focus traversal, sub-page navigation, and top/bottom screen interaction handoffs on the AYN Thor dual-screen handheld. Use when: developing or debugging features that transition focus between Display 0 (Top Overlay) and Display 4 (Bottom Companion/Canvas/Mirror)."
argument-hint: "Feature or subpage to audit/implement (e.g. 'button movement', 'background crop', 'viewport edit')"
---

# Skill: Dual-Screen Gamepad Focus & Interaction Architecture

> **Role & Persona:** You are an expert Android/Compose engineer specializing in gamepad-first dual-screen navigation and focus traversal for the **Megingiard** AYN Thor companion application. Your goal is to ensure deterministic, console-grade focus handoff between the Top Screen (Display 0 `WindowManager` overlay) and the Bottom Screen (Display 4 `MainActivity` / `PadCanvas` / `EmbeddedMirrorView`), with 100% reliable focus recovery on cancel/completion.

---

## 1. Project & Dual-Screen Hardware Context

| Key | Value |
| --- | --- |
| **Device Target** | AYN Thor dual-screen handheld |
| **Top Display (Display 0)** | 1920×1080 60Hz. Hosts foreground games / emulators + non-Activity `WindowManager` overlays (`PrimaryOverlayManager`, `MacroPadEditor`, `GlobalSettingsScreen`, `MirrorEditorTopOverlay`). |
| **Bottom Display (Display 4)** | 1080×1240 60Hz capacitive multi-touch. Hosts `MainActivity` (`PadCanvas`, `PadTableGrid`, `EmbeddedMirrorView`, `PadSurface`). |
| **Interaction Rule** | **Top Screen:** Gamepad-First (Zero touch requirement). **Bottom Screen:** Touch & Gamepad hybrid. |
| **Input Pipeline** | Physical Gamepad → `PrimaryOverlayManager` (`setOnGenericMotionListener` / `setOnKeyListener`) → `PrimaryOverlayInputBridge` → Compose Focus Tree. |

---

## 2. Comprehensive Map of Dual-Screen Features & Focus Handoffs

Every dual-screen flow in Megingiard falls into one of the following canonical patterns:

### 2.1 Pattern A: Top Screen Triggers Bottom Screen Interactive Mode (Moving / Cropping)

When an action on the Top Screen puts the Bottom Screen into an interactive mode (e.g. background cropping, precision button moving):

1. **Active State Flag:** Hoisted in `MacroPadState` (e.g. `isCroppingBackground`, `isEditingButtonPositions`, `movingButtonId`).
2. **Dedicated BackHandler:** The parent subpage **MUST** register an active `BackHandler(enabled = isModeActive)`:
   - Pressing Gamepad `(B)`, `Back`, or `Escape` immediately deactivates the mode (`isModeActive = false`).
   - Focus is synchronously restored to the triggering card via `FocusRequester.requestFocus()`.
   - Under NO circumstance should pressing `(B)` in an active interactive mode bubble up or pop the parent subpage.
3. **Synchronous Visual Feedback:** The bottom display border (`PadCanvas` or `PadTableGrid`) highlights with an active accent/cyan border (`floatScale = 1.04f` / `shadowElevation = 16f`), preserving the underlying radial gradient colors.

| Feature | Triggering Screen | Bottom Screen Receiver | Exit / Cancel Action | Focus Recovery Target |
| --- | --- | --- | --- | --- |
| **Button Movement** | `EditButtonPositionsSubPageContent` | `PadCanvas` / `PadTableGrid` (drag handles / cell swap) | `(B)` / `(A)` / Enter / Touch release | Corresponding `btn.id` card |
| **Background Crop** | `LayoutBackgroundSubPageContent` | `PadCanvas` (pinch-to-zoom 100%–500%, pan) | `(B)` / Toggle click | `cropCardFocusRequester` |
| **Viewport / Cutouts** | `MacroPadEditor` (Mirror section) | `ViewportTransformOverlay` / `EmbeddedMirrorView` | `(B)` / Done / Cancel | "Edit Cutouts" Action Card |

---

### 2.2 Pattern B: Deep Deck Drilling & Pop Focus Restoration

When drilling into sub-pages (`MacroPadSubPage`) or child item editors:

1. **Unique Focus Key (`itemKey`):** Every dynamic card in a `LazyColumn` or `Column` MUST specify an explicit, unique `itemKey` (e.g. `itemKey = btn.id`, `itemKey = layout.id`, `itemKey = profile.id`). **Never rely on the default `itemKey = title`**, which collides when items share identical titles.
2. **Navigation Stack Tracking:** `GamepadTwoPaneScaffold` records `savedFocusKeysByDepth[depth] = itemKey`.
3. **Depth-Change Synchronous Focus:** When popping back (`subPageStack.dropLast(1)`):
   - `GamepadTwoPaneScaffold` retrieves `savedFocusKeysByDepth[newDepth]`.
   - It searches `activeDeckCardRequesters[key]` and synchronously invokes `requestFocus()`.
   - If unattached on the first tick (e.g. item needs lazy scrolling), it brings the item into view via `rememberGamepadBringIntoViewSpec` and retries.

| Subpage Flow | Parent Section | Sub-Page Content | Focus Return Key |
| --- | --- | --- | --- |
| **Edit Button** | `ButtonsDeck` | `EditButtonSubPageContent` | `button.id` |
| **Choose Icon** | `EditButtonSubPageContent` | `ChooseIconSubPageContent` | Icon Action Card |
| **Choose App** | `EditButtonSubPageContent` | `ChooseAppSubPageContent` | App Launcher Card |
| **Color Wheel** | `EditButton` / `LayoutColor` | `ColorWheelSubPageContent` | Target Color Card |
| **Edit Layout** | `LayoutsDeck` | `EditLayoutSubPageContent` | `layout.id` |
| **Edit Profile** | `ProfilesDeck` | `EditProfileSubPageContent` | `profile.id` |

---

### 2.3 Pattern C: Overlay Window Minimization & Modal Handoffs

When a task requires temporarily relinquishing the Top Screen overlay (e.g. physical macro recording or app quick switching):

1. **Clean State Suspension:** `PrimaryOverlayManager` closes or minimizes the overlay (`AppStateManager.closePrimaryModal()`).
2. **Foreground Preservation:** The target task interacts with the device / game without interference.
3. **Re-Opening with Deep Link Payload:** Upon task completion/cancellation:
   - Call `AppStateManager.openPrimaryModal(PrimaryModalConfig(..., payload = PrimaryModalPayload.MacroPad(...)))`.
   - `MacroPadNavState.applyPrimaryModalPayload` navigates directly to the target section/subpage and focuses the remembered card.

---

### 2.4 Pattern D: External SAF / Document / Image Picker Handoffs

When Top Screen dialogs trigger an external Android file/image picker (e.g. SAF `GetContent` or `CreateDocument` on Display 4):

1. **Input Interception Bypass (`isExternalPickerActive`):** `MainActivity` flags `isExternalPickerActive = true` immediately before launching `ActivityResultLauncher`. While active, `MainActivity.dispatchKeyEvent()` strictly passes through all Gamepad key events (`super.dispatchKeyEvent(event)`) without forwarding them to `PrimaryOverlayManager`. This guarantees the user can freely navigate the bottom-screen file picker in 2D (Left/Right/Up/Down/A/B) without the top overlay consuming D-pad events.
2. **Synchronous Focus Return:** In the picker result callback:
   - Clear `isExternalPickerActive = false`.
   - Invoke `PrimaryOverlayManager.requestFocus()`.
   - Trigger `PrimaryOverlayInputBridge.sendFocusRecovery(KeyEvent.KEYCODE_DPAD_DOWN)` to deterministically re-anchor Gamepad focus to the Top Screen overlay upon cancel or completion.

---

## 3. Systematic Dual-Screen Focus Checklist

When implementing or reviewing any dual-screen feature in the ZH branch, verify:

- [ ] **Unique Item Keys:** Every `GamepadActionCard`, `GamepadFocusCard`, or `GamepadToggleCard` in a dynamic list has an explicit `itemKey` (e.g. `itemKey = item.id`).
- [ ] **BackHandler on Interactive Modes:** Any mode that allows bottom-screen interaction has an active `BackHandler(enabled = isModeActive)` to cancel mode and restore top-screen card focus on `(B)` press.
- [ ] **External Picker Input Bypass:** All SAF / ActivityResult launchers set `isExternalPickerActive = true` to allow full 2D Gamepad navigation in the bottom file picker, and request focus recovery upon dismissal.
- [ ] **Gradient Preservation:** Selection highlights on the bottom screen (e.g. `PadTableCell`) use borders (`cellBorderColor`), scaling (`floatScale`), and shadow elevation (`shadowElevation`), while preserving the button's configured `bgColor` (`effectiveBg`) radial gradient.
- [ ] **Overlay-Safe Pickers:** All system file / image pickers use domain coordinator singletons (`ButtonImagePickerManager`, `BackgroundPickerManager`) collected by `MainActivity`, never `rememberLauncherForActivityResult` in overlay composables.
- [ ] **Touch-to-Top Sync:** Touching an element on the bottom screen (e.g. table cell or canvas button) sets `MacroPadState.selectedButtonId`, which automatically scrolls and requests focus on the corresponding top-screen card.
- [ ] **Focus Recovery Listener:** Top overlay composables listen to `PrimaryOverlayInputBridge.focusRecoveryEvents` to recover focus on unhandled D-pad inputs.

---

## 4. Output & Commit Message Format

Always conclude with a **Conventional Commits** message proposal covering all changed files:

```commit
<type>(macropad): <short imperative summary>

- bullet 1
- bullet 2
```

---
name: megingiard-release-zh
description: "Manage Megingiard-ZH releases, trigger automated GitHub Actions builds, update release notes with fork additions, and verify release artifacts."
argument-hint: "Optional: version tag (e.g. v0.9.0-zh)"
---

# Skill: Megingiard — Enhanced Chinese Edition Release Manager

## Role

You are the release engineer for **Megingiard-ZH**. Your task is to maintain release notes, trigger GitHub Actions build workflows for `megingiard-vX.Y.Z-zh.apk`, update GitHub release assets, and verify release package hashes.

---

## Release Notes Rules (Strict Zero-Noise Policy)
The release notes MUST only highlight fork-specific additions (do NOT restate upstream features):
- **獨立 Package ID**：`com.stormpanda.megingiard.zh`（可與官方原版並存安裝，互不干擾衝突）
- **巨集板表格模式**：自訂網格行列（1×1 至 8×6）、儲存格長按拖曳交換/移動、跨格按鈕、頂層格線繪製
- **按鈕圖片雙螢幕裁切**：支援按鈕自訂圖片，並在副螢幕/主螢幕提供放大裁切畫布與滿版（Full Bleed）顯示
- **繁體中文在地化**：100% 全介面繁體中文支援

---

## Execution Workflow

### Step 1: Verify Release Notes in Workflow & README
Ensure `.github/workflows/build-apk.yml`, `README.md`, and `README_en.md` reflect the exact 4 highlights above.

### Step 2: Trigger GitHub Actions Workflow
Trigger the release build on GitHub Actions via `gh workflow run`:
```bash
gh workflow run build-apk.yml --repo yangyws/megingiard-zh --ref main-zh
```

### Step 3: Monitor Build Run
Check run progress until completion:
```bash
gh run list --repo yangyws/megingiard-zh --limit 1
```

### Step 4: Sync GitHub Release Notes
Ensure the release notes on `https://github.com/yangyws/megingiard-zh/releases` match the clean 4 highlights using `gh release edit`:
```bash
gh release edit v0.10.0-zh --repo yangyws/megingiard-zh --notes "<clean markdown body>"
```

### Step 5: Verify Artifact Integrity
Download and inspect the published release APK:
- Verify package ID is `com.stormpanda.megingiard.zh`
- Verify internal VCS revision hash matches latest commit on `main-zh`

---

## Output Requirements
- Provide direct link to GitHub Actions run.
- Provide direct link to GitHub Release download.

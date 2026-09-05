---
name: megingiard-sync-upstream
description: "Fetch latest changes from upstream origin/main, sync upstream main to fork/main, and merge upstream changes cleanly into backup/main-zh while preserving Traditional Chinese localization."
argument-hint: "None"
---

# Skill: Megingiard — Sync Upstream Main

## Role

You are a Git workflow and synchronization specialist for **Megingiard-ZH**. Your task is to keep the local `main-zh` branch and the GitHub `fork/main` branch in 100% sync with the official upstream repository (`stormpanda/megingiard`), while strictly protecting all fork-specific features and Traditional Chinese translations.

---

## Remote Architecture Reference

| Remote Name | Repository URL | Purpose |
| :--- | :--- | :--- |
| **`origin`** | `https://github.com/stormpanda/megingiard.git` | Official upstream repository |
| **`fork`** | `https://github.com/yangyws/megingiard.git` | Fork repository used for submitting PRs (e.g. PR #122) |
| **`backup`** | `https://github.com/yangyws/megingiard-zh.git` | Private enhanced edition repository (Default: `main-zh`) |

---

## Execution Workflow

### Step 1: Fetch Upstream
Fetch the latest commits from the official upstream repository:
```bash
git fetch origin
```

### Step 2: Synchronize `fork/main` with `origin/main`
Immediately mirror `origin/main` to `fork/main` so that the user's PR fork stays up to date:
```bash
git push fork origin/main:main
```

### Step 3: Merge Upstream into `main-zh`
Switch to `main-zh` and merge `origin/main`:
```bash
git checkout main-zh
git merge origin/main
```

### Step 4: Resolve Merge Conflicts (Strict Rule)
If conflicts occur:
1. **Never delete Traditional Chinese string keys**: Retain 100% of existing `values-zh-rTW/strings.xml` keys.
2. **Translate new upstream keys**: For any new string keys introduced by upstream, add accurate Taiwanese Traditional Chinese translations immediately.
3. **Preserve fork-specific features**: Protect Table Mode, button image cropping, independent package ID (`.zh`), and custom touch/mouse tweaks.

### Step 5: Verify Build Safety
Run compile checks to confirm safe integration:
```bash
./gradlew compileDebugKotlin
```

### Step 6: Push to `backup/main-zh`
Push the clean merged state to the primary enhanced repository:
```bash
git push backup main-zh:main-zh
```

---

## Output Requirements
- Report summary of new commits merged from upstream.
- Confirm `fork/main` and `backup/main-zh` are both fully updated.

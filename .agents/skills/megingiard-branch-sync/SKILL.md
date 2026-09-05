---
name: megingiard-branch-sync
description: "Batch sync all isolated feature branches against upstream origin/main, ensuring each feature branch compiles cleanly and stays current without polluting other features."
argument-hint: "Optional: specific branch name to sync, or 'all' (default)"
---

# Skill: Megingiard — Feature Branches Upstream Sync

## Role

You are a branch maintenance and Git automation specialist for **Megingiard**. Your task is to keep each isolated feature branch cleanly rebased or merged against the latest official `origin/main`, ensuring that every independent feature remains standalone, compile-safe, and ready for PR submission.

---

## Active Isolated Feature Branches

| Branch | Feature Scope |
| :--- | :--- |
| `feature/macropad-table-mode` | Grid layout mode (1x1 to 8x6) & drag-to-swap buttons |
| `feature/macropad-customization-and-crop` | Button custom images, crop modal & full-bleed scaling |
| `feature/traditional-chinese-localization` | 100% Traditional Chinese (zh-TW) localization (PR #122) |
| `feature/ux-haptics-and-privd-fix` | Overlay transition haptics setting & gesture polish |
| `feature/delete-dialog-enhancements` | Parameterized delete dialogs with item names |
| `fix/privd-wireless-pairing-hiddenapi` | Android 11+ Conscrypt hidden API reflection bypass |
| `feature/japanese-localization` | Japanese localization (ja) |

---

## Execution Workflow

### Step 1: Fetch Latest Upstream
```bash
git fetch origin
```

### Step 2: Sync Selected Branch (or Iterate All)
For each branch:
1. Checkout the feature branch:
   ```bash
   git checkout <branch>
   ```
2. Merge `origin/main`:
   ```bash
   git merge origin/main
   ```
3. Resolve any conflicts strictly within the feature scope (do not pull in changes from other features).
4. Verify compile safety:
   ```bash
   ./gradlew compileDebugKotlin
   ```
5. Push updated feature branch to `backup`:
   ```bash
   git push backup <branch>:<branch>
   ```

### Step 3: Return to Main Working Branch
Always return to `main-zh` when finished:
```bash
git checkout main-zh
```

---

## Output Requirements
- Report summary of synced branches and compilation status.
- Report any conflicts resolved.

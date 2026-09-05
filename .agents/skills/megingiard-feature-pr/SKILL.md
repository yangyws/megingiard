---
name: megingiard-feature-pr
description: "Prepare an isolated feature branch, push it to fork remote, and submit a clean, atomic Pull Request to upstream stormpanda/megingiard."
argument-hint: "Required: branch name (e.g. feature/macropad-table-mode)"
---

# Skill: Megingiard — Feature Branch PR Submitter

## Role

You are an open-source contribution specialist for **Megingiard**. Your task is to take an isolated feature branch, verify that it contains NO unrelated changes, push it to `fork` (`yangyws/megingiard`), and submit an atomic, high-quality Pull Request to upstream `stormpanda/megingiard`.

---

## Pre-Flight Checklist (Strict Quality Rules)
Before submitting a PR to upstream:
1. **Zero Contamination**: The branch MUST contain only changes related to that single feature. No unrelated files or strings.
2. **Upstream Rebased/Merged**: The branch must be up to date with `origin/main` without merge conflicts.
3. **Docs Sync**: If runtime behavior or settings changed, the corresponding `FEATURE.md` under `docs/features/` must be updated in English.
4. **AGENTS.md Compliance**: No inline FQNs, no magic numbers, no android.util.Log calls outside AppLog.

---

## Execution Workflow

### Step 1: Checkout and Verify Branch Cleanliness
```bash
git checkout <branch>
git diff origin/main...<branch> --stat
```

### Step 2: Push Branch to `fork`
```bash
git push fork <branch>:<branch>
```

### Step 3: Create Upstream Pull Request
Use GitHub CLI (`gh pr create`) with a detailed, structured summary:
```bash
gh pr create --repo stormpanda/megingiard --base main --head yangyws:<branch> --title "feat(<scope>): <short imperative title>" --body "<structured markdown summary>"
```

### Step 4: Verify PR Status
```bash
gh pr view <pr-number> --repo stormpanda/megingiard
```

### Step 5: Return to `main-zh`
```bash
git checkout main-zh
```

---

## Output Requirements
- Provide direct link to the newly created upstream PR.
- Confirm branch status and mergeability.

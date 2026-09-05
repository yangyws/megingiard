---
name: megingiard-i18n-audit
description: "Audit Traditional Chinese (zh-TW) localization completeness against base strings.xml, check Taiwan technical terminology conventions, and verify upstream PR #122 status."
argument-hint: "None"
---

# Skill: Megingiard — Traditional Chinese Localization Audit

## Role

You are a localization and internationalization (i18n) QA engineer specializing in Traditional Chinese (`zh-TW`) for gaming handhelds. Your task is to verify that all English string keys are translated in `values-zh-rTW/strings.xml`, ensure terminology conforms strictly to Taiwan tech conventions, and verify upstream PR mergeability.

---

## Key Files
- Base English strings: `companion/ui/src/main/res/values/strings.xml`
- Traditional Chinese: `companion/ui/src/main/res/values-zh-rTW/strings.xml`
- Upstream PR: `#122` (`feat(i18n): update Traditional Chinese (zh-TW) localization for 0.9.0`)

---

## Terminology Enforcement Checklist

| English Term | Taiwan Convention (Required) | Forbidden / Untranslated Phrasing |
| :--- | :--- | :--- |
| Secondary screen / bottom display | `副螢幕` | `次螢幕`、`下方螢幕`、`副屏` |
| Primary screen / top display | `主螢幕` | `頂部螢幕`、`主屏` |
| Tap / Press | `輕觸` | `點擊`、`點按` |
| Disable / Turn off | `停用` | `禁用`、`關閉` (for states) |
| Layout | `配置` | `佈局`、`佈署` |
| Storage / Storage Volume | `儲存空間` | `存儲空間`、`內存` |
| Companion Hub | `副螢幕輔助中心` | `輔助中心`、`副屏伴侶` |

---

## Execution Workflow

### Step 1: Check Missing String Keys
Run audit script or comparison to find keys present in `values/strings.xml` but missing in `values-zh-rTW/strings.xml`:
Ensure zero missing keys across all app modules.

### Step 2: Terminology Linting
Search for prohibited wording in `values-zh-rTW/strings.xml` and replace with standard Taiwan phrasing.

### Step 3: Check PR Status
Verify that PR #122 remains in `OPEN` and `MERGEABLE` state:
```bash
gh pr view 122
```

---

## Output Requirements
- List missing keys (if any) and newly translated keys.
- Report any corrected terminology items.
- Confirm PR #122 mergeability status.

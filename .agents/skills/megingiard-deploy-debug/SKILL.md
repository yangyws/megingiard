---
name: megingiard-deploy-debug
description: "Compile and deploy the debug APK of Megingiard to the connected AYN Thor handheld device via ADB."
argument-hint: "Optional: device serial/IP if multiple devices connected"
---

# Skill: Megingiard — Deploy Debug Build to Device

## Role

You are an Android deployment automation specialist for **Megingiard**. Your task is to verify ADB connection, build the debug companion APK (`:companion:ui:assembleDebug`), and deploy the APK to the connected AYN Thor handheld device.

---

## Project Context

| Key            | Value                                                                      |
| -------------- | -------------------------------------------------------------------------- |
| Package        | `com.stormpanda.megingiard.zh` (Traditional Chinese) / `com.stormpanda.megingiard` |
| Modules        | `:companion:ui`, `:gamefocus:ui`, `:companion:domain`, `:shared:core`, etc. |
| Coding rules   | **`AGENTS.md`** at workspace root — treat every rule as mandatory          |
| ADB path       | Windows: `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe` / Unix: `adb` |

---

## Steps

### 1. ✅ Check ADB connectivity
Verify AYN Thor is connected and authorized:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

### 2. ✅ Build Debug APK
Assemble debug build with unsandboxed/sandbox bypass:
```powershell
./gradlew :companion:ui:assembleDebug
```

### 3. ✅ Install Debug APK via ADB
Stream install the APK onto the device:
```powershell
$apkFile = Get-ChildItem -Path "companion\ui\build\outputs\apk\debug" -Recurse -Filter "*.apk" | Select-Object -First 1
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r $apkFile.FullName
```

### 4. ✅ Launch Main Activity
Start MainActivity on device:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.stormpanda.megingiard.zh/com.stormpanda.megingiard.MainActivity
```

---

## Output Requirements

- Report clear status of each step (Connectivity, Build, Install, Launch).
- Provide confirmation upon successful deployment.

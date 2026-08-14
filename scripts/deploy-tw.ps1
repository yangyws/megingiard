$ErrorActionPreference = "Stop"

$adbExe = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (!(Test-Path $adbExe)) {
    $adbExe = "adb"
}

Write-Host "========================================="
Write-Host " [1/4] Running Pure Core Unit Tests..."
Write-Host "========================================="
./gradlew :shared:core:test
if ($LASTEXITCODE -ne 0) {
    Write-Error "[ERROR] Unit tests failed! Deployment aborted."
    exit 1
}

Write-Host "========================================="
Write-Host " [2/4] Building Debug APK..."
Write-Host "========================================="
./gradlew :companion:ui:assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Error "[ERROR] APK build failed! Deployment aborted."
    exit 1
}

Write-Host "========================================="
Write-Host " [3/4] Backing up APK..."
Write-Host "========================================="
$backupDir = "D:\test-apk"
if (!(Test-Path $backupDir)) {
    New-Item -ItemType Directory -Path $backupDir -Force
}
$apkFile = Get-ChildItem -Path "companion\ui\build\outputs\apk" -Recurse -Filter "*.apk" | Select-Object -First 1
if ($null -eq $apkFile) {
    Write-Error "[ERROR] Built APK file not found under companion\ui\build\outputs\apk!"
    exit 1
}
Copy-Item $apkFile.FullName "$backupDir\megingiard-tw-debug.apk" -Force
Write-Host "APK File: $($apkFile.FullName)"

Write-Host "========================================="
Write-Host " [4/4] Deploying to AYN Thor via ADB..."
Write-Host "========================================="
& $adbExe install -r $apkFile.FullName
& $adbExe shell am start -n com.stormpanda.megingiard.debug/com.stormpanda.megingiard.MainActivity

Write-Host "========================================="
Write-Host " [SUCCESS] Build & Deployment Complete!"
Write-Host "========================================="

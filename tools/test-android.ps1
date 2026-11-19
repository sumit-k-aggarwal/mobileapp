param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\android-env.ps1"
Push-Location $projectRoot
try {
    if (!$SkipBuild) {
        & .\gradlew.bat --console=plain :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
        if ($LASTEXITCODE -ne 0) { throw 'Android build or checks failed.' }
    }
    & "$PSScriptRoot\run-android.ps1" -Headless -NoLaunch
    $adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
    & $adb -s emulator-5580 install -r '.\app\build\outputs\apk\debug\app-debug.apk'
    if ($LASTEXITCODE -ne 0) { throw 'App installation failed.' }
    & $adb -s emulator-5580 install -r '.\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
    if ($LASTEXITCODE -ne 0) { throw 'Test APK installation failed.' }
    $result = & $adb -s emulator-5580 shell am instrument -w com.glide.taxi.test/com.glide.taxi.RideInstrumentation
    New-Item '.\artifacts' -ItemType Directory -Force | Out-Null
    $result | Set-Content '.\artifacts\android-tests.txt'
    $result | Write-Output
    if (!($result -match '^OK: \d+ Android checks')) { throw 'Android acceptance checks failed. See artifacts\android-tests.txt.' }
} finally {
    Pop-Location
}

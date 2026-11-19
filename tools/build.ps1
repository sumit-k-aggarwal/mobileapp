$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\android-env.ps1"
Push-Location $projectRoot
try {
    & .\gradlew.bat --console=plain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
    if ($LASTEXITCODE -ne 0) { throw 'Android build or checks failed.' }
    New-Item '.\dist' -ItemType Directory -Force | Out-Null
    Copy-Item '.\app\build\outputs\apk\debug\app-debug.apk' '.\dist\Glide-debug.apk' -Force
    Write-Output "APK: $projectRoot\dist\Glide-debug.apk"
} finally {
    Pop-Location
}

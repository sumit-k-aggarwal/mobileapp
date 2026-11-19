param(
    [switch]$Headless,
    [switch]$NoLaunch
)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\android-env.ps1"
$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
$serial = 'emulator-5580'
$device = & $adb devices
if (!($device -match "$serial\s+device")) {
    if (!(Test-Path "$env:ANDROID_AVD_HOME\Glide_Pixel.ini")) {
        'no' | & "$env:ANDROID_HOME\cmdline-tools\latest\bin\avdmanager.bat" create avd `
            --name Glide_Pixel --package 'system-images;android-35;google_apis;x86_64' --device pixel_6
        if ($LASTEXITCODE -ne 0) { throw 'Could not create Glide_Pixel test bed.' }
    }
    $arguments = @('-avd', 'Glide_Pixel', '-port', '5580', '-no-snapshot', '-no-boot-anim',
        '-gpu', 'swiftshader', '-no-audio', '-memory', '2048', '-camera-back', 'none', '-camera-front', 'none')
    if ($Headless) { $arguments += '-no-window' }
    Start-Process "$env:ANDROID_HOME\emulator\emulator.exe" -ArgumentList $arguments | Out-Null
}
$deadline = (Get-Date).AddMinutes(3)
do {
    $boot = & $adb -s $serial shell getprop sys.boot_completed 2>$null
    if ($boot -eq '1') { break }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $deadline)
if ($boot -ne '1') { throw 'The Glide Android emulator did not boot within 3 minutes.' }
$avdName = & $adb -s $serial emu avd name
if ($avdName[0] -ne 'Glide_Pixel') { throw 'Port 5580 belongs to another test bed; leaving it unchanged.' }
& $adb -s $serial shell input keyevent 82
if ($NoLaunch) { return }
$apk = "$projectRoot\app\build\outputs\apk\debug\app-debug.apk"
if (!(Test-Path $apk)) { throw 'Build the app first with tools\build.ps1.' }
& $adb -s $serial install -r $apk
if ($LASTEXITCODE -ne 0) { throw 'APK installation failed.' }
& $adb -s $serial shell am start -W -n com.glide.taxi/.MainActivity
if ($LASTEXITCODE -ne 0) { throw 'App launch failed.' }
Write-Output "Glide is running on $serial (Glide_Pixel)."

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$tools = Join-Path $root '.tools'
New-Item $tools -ItemType Directory -Force | Out-Null
function Download($url, $file) {
    if (!(Test-Path $file)) {
        & curl.exe -fL --retry 3 --silent --show-error $url -o $file
        if ($LASTEXITCODE -ne 0) { throw "Download failed: $url" }
    }
}
Download 'https://aka.ms/download-jdk/microsoft-jdk-21-windows-x64.zip' "$tools\jdk.zip"
if (!(Test-Path "$tools\jdk")) {
    Expand-Archive "$tools\jdk.zip" "$tools\java-unpack" -Force
    Move-Item (Get-ChildItem "$tools\java-unpack" -Directory | Select-Object -First 1).FullName "$tools\jdk"
}
$env:JAVA_HOME = "$tools\jdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Download 'https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip' "$tools\commandline.zip"
$sdk = "$tools\android-sdk"
if (!(Test-Path "$sdk\cmdline-tools\latest\bin\sdkmanager.bat")) {
    Expand-Archive "$tools\commandline.zip" "$tools\cli-unpack" -Force
    New-Item "$sdk\cmdline-tools" -ItemType Directory -Force | Out-Null
    Move-Item "$tools\cli-unpack\cmdline-tools" "$sdk\cmdline-tools\latest"
}
$env:ANDROID_HOME = $sdk
$env:ANDROID_USER_HOME = "$tools\android-user"
$env:ANDROID_AVD_HOME = "$tools\avd"
New-Item $env:ANDROID_USER_HOME,$env:ANDROID_AVD_HOME -ItemType Directory -Force | Out-Null
Download 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip' "$tools\gradle.zip"
if (!(Test-Path "$tools\gradle-8.11.1")) { Expand-Archive "$tools\gradle.zip" $tools -Force }
$manager = "$sdk\cmdline-tools\latest\bin\sdkmanager.bat"
1..100 | ForEach-Object { 'y' } | & $manager "--sdk_root=$sdk" --licenses | Out-Null
& $manager "--sdk_root=$sdk" 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0' 'emulator' 'system-images;android-35;google_apis;x86_64'
if ($LASTEXITCODE -ne 0) { throw 'Android SDK installation failed' }
& "$tools\gradle-8.11.1\bin\gradle.bat" --version
& "$sdk\emulator\emulator.exe" -accel-check
Write-Output "Android tools ready in $tools"

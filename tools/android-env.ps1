$projectRoot = Split-Path $PSScriptRoot -Parent
$env:JAVA_HOME = "$projectRoot\.tools\jdk"
$env:ANDROID_HOME = "$projectRoot\.tools\android-sdk"
$env:ANDROID_USER_HOME = "$projectRoot\.tools\android-user"
$env:ANDROID_AVD_HOME = "$projectRoot\.tools\avd"
$env:GRADLE_USER_HOME = "$projectRoot\.tools\gradle-home"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\emulator;$env:PATH"

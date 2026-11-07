# Run from a PowerShell terminal with JDK 17 and Android SDK 35 installed.
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (Test-Path -LiteralPath './gradlew.bat') {
    & './gradlew.bat' ':app:assembleDebug'
} elseif (Get-Command gradle -ErrorAction SilentlyContinue) {
    & gradle wrapper --gradle-version 8.9 --distribution-type bin
    if ($LASTEXITCODE -ne 0) { throw 'Gradle wrapper generation failed.' }
    & './gradlew.bat' ':app:assembleDebug'
} else {
    throw 'Install Gradle 8.9 or open this project in Android Studio and select Gradle 8.9. See README.md.'
}
if ($LASTEXITCODE -ne 0) { throw 'Android build failed. Read the Gradle output.' }

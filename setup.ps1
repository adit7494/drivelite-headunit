[CmdletBinding()]
param([switch]$AdbOnly, [string]$CacheDirectory)
$ErrorActionPreference = 'Stop'
if ($env:OS -ne 'Windows_NT') { throw 'Setup ini untuk Windows x64.' }
$config = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'config\toolchain.json') -Raw | ConvertFrom-Json
$toolsRoot = Join-Path $PSScriptRoot '.tools'
if (-not $CacheDirectory) { $CacheDirectory = Join-Path $toolsRoot 'downloads' }
New-Item -ItemType Directory -Path $toolsRoot, $CacheDirectory -Force | Out-Null
$items = if ($AdbOnly) { @($config.adb) } else { @($config.jdk, $config.buildTools, $config.platform, $config.adb) }
Write-Host 'Mengunduh paket resmi Google Android SDK dan Eclipse Temurin; tidak mengubah PATH sistem.'
foreach ($item in $items) {
    $archive = Join-Path $CacheDirectory $item.file
    if (-not (Test-Path -LiteralPath $archive)) {
        Write-Host "Download $($item.file)"
        Invoke-WebRequest -Uri $item.url -OutFile $archive
    }
    if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash -ne $item.sha256) {
        throw "Checksum tidak sesuai: $archive. Hapus arsip itu lalu ulangi setup."
    }
    $destination = Join-Path $toolsRoot $item.destination
    New-Item -ItemType Directory -Path $destination -Force | Out-Null
    Write-Host "Verifikasi OK; ekstrak $($item.file)"
    Expand-Archive -LiteralPath $archive -DestinationPath $destination -Force
}
Write-Host 'Setup selesai. ADB: .\.tools\platform-tools\adb.exe'
if (-not $AdbOnly) { Write-Host 'Berikutnya: .\build.ps1 -Artwork Demo' }

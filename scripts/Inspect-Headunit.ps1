[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Serial,
    [ValidateRange(0, 100000)][int]$UserId = 0,
    [switch]$Connect,
    [string]$OutputDirectory,
    [string]$AdbPath
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'Common.ps1')
$adb = if ($AdbPath) { (Resolve-Path -LiteralPath $AdbPath).Path } else { Get-DriveLiteAdb $projectRoot }
if ($Connect) {
    & $adb connect $Serial
    Assert-DriveLiteExit 'ADB connect'
}
$state = ((& $adb -s $Serial get-state 2>&1) -join '').Trim()
if ($LASTEXITCODE -ne 0 -or $state -ne 'device') {
    throw 'Perangkat belum berstatus device. Periksa alamat dan dialog Allow pada head unit; belum ada inventaris yang dibuat.'
}
if (-not $OutputDirectory) {
    $stamp = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 6)
    $OutputDirectory = Join-Path $projectRoot ('output\headunit\' + $stamp)
}
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
if (Test-Path -LiteralPath $OutputDirectory) { throw 'Gunakan folder output baru agar hasil audit lama tidak tertimpa.' }
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$script:captureResults = [Collections.Generic.List[object]]::new()
function Read-Headunit([string]$File, [string[]]$Arguments, [switch]$Required) {
    $lines = @(& $adb -s $Serial @Arguments 2>&1 | ForEach-Object { "$_" })
    $code = $LASTEXITCODE
    $text = $lines -join "`n"
    [IO.File]::WriteAllText((Join-Path $OutputDirectory $File), $text, [Text.UTF8Encoding]::new($false))
    $ok = $code -eq 0 -and $text -notmatch '(?im)^\s*(error:|Error:|Unknown command:|SecurityException|Exception occurred)'
    $script:captureResults.Add([pscustomobject]@{ file = $File; arguments = $Arguments; exitCode = $code; available = $ok })
    if (-not $ok -and $Required) { throw "Pembacaan wajib gagal: $File. Periksa file itu; inventaris belum lengkap." }
    if (-not $ok) { Write-Warning "Pembacaan tambahan tidak tersedia: $File. Lihat capture-status.json." }
    return $text
}
function Package-Names([string]$Text) {
    return @([regex]::Matches($Text, '(?m)^package:([A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)*)\s*$') |
        ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique)
}
try {
    Write-Host 'Membaca inventaris user dan metadata; tidak mengubah aplikasi atau pengaturan head unit.'
    $installed = @(Package-Names (Read-Headunit 'installed.txt' @('shell', 'pm', 'list', 'packages', '--user', "$UserId") -Required))
    $known = @(Package-Names (Read-Headunit 'all-known.txt' @('shell', 'pm', 'list', 'packages', '-u', '--user', "$UserId") -Required))
    $disabled = @(Package-Names (Read-Headunit 'disabled.txt' @('shell', 'pm', 'list', 'packages', '-d', '--user', "$UserId") -Required))
    $enabled = @(Package-Names (Read-Headunit 'enabled.txt' @('shell', 'pm', 'list', 'packages', '-e', '--user', "$UserId") -Required))
    $system = @(Package-Names (Read-Headunit 'system.txt' @('shell', 'pm', 'list', 'packages', '-s', '-u', '--user', "$UserId") -Required))
    if ($installed.Count -eq 0 -or $known.Count -eq 0) { throw 'Daftar paket kosong/tidak dikenali. Inventaris tidak boleh dipakai sebagai dasar perubahan.' }
    $device = [ordered]@{
        model = (Read-Headunit 'model.txt' @('shell', 'getprop', 'ro.product.model')).Trim()
        android = (Read-Headunit 'android.txt' @('shell', 'getprop', 'ro.build.version.release')).Trim()
        sdk = (Read-Headunit 'sdk.txt' @('shell', 'getprop', 'ro.build.version.sdk')).Trim()
        abi = (Read-Headunit 'abi.txt' @('shell', 'getprop', 'ro.product.cpu.abi')).Trim()
        currentUser = (Read-Headunit 'current-user.txt' @('shell', 'am', 'get-current-user')).Trim()
        targetUser = $UserId
    }
    $null = Read-Headunit 'users.txt' @('shell', 'pm', 'list', 'users')
    $null = Read-Headunit 'screen.txt' @('shell', 'wm', 'size')
    $null = Read-Headunit 'home.txt' @('shell', 'cmd', 'package', 'resolve-activity', '--brief', '--user', "$UserId", '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME')
    $null = Read-Headunit 'webview.txt' @('shell', 'dumpsys', 'webviewupdate')
    $null = Read-Headunit 'memory.txt' @('shell', 'dumpsys', 'meminfo')
    $null = Read-Headunit 'processes.txt' @('shell', 'ps')
    $null = Read-Headunit 'packages.txt' @('shell', 'dumpsys', 'package')
    $animations = [ordered]@{}
    foreach ($key in @('window_animation_scale', 'transition_animation_scale', 'animator_duration_scale')) {
        $animations[$key] = (Read-Headunit ($key + '.txt') @('shell', 'settings', 'get', 'global', $key)).Trim()
    }
    $animations | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $OutputDirectory 'animations-before.json') -Encoding utf8
    $apps = @(@($known + $installed) | Sort-Object -Unique | ForEach-Object {
        $name = $_
        $isInstalled = $name -in $installed
        $userState = if (-not $isInstalled) { 'not-installed' } elseif ($name -in $disabled) { 'disabled' } elseif ($name -in $enabled) { 'enabled' } else { 'unknown' }
        [pscustomobject]@{ package = $name; installed = $isInstalled; state = $userState; systemApp = $name -in $system }
    })
    $inventory = [ordered]@{
        schemaVersion = 1
        createdUtc = (Get-Date).ToUniversalTime().ToString('o')
        serial = $Serial
        device = $device
        installedCount = $installed.Count
        knownCount = $apps.Count
        packages = $apps
        note = 'Status sistem bukan rekomendasi penghapusan. Baca metadata, fungsi, dependensi, dan kebutuhan pengguna; label aplikasi belum dikumpulkan.'
    }
    $inventory | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $OutputDirectory 'inventory.json') -Encoding utf8
    $apps | Export-Csv -LiteralPath (Join-Path $OutputDirectory 'packages.csv') -NoTypeInformation -Encoding utf8
    Write-Host "Inventaris selesai: $OutputDirectory"
    Write-Host "Terpasang: $($installed.Count); dikenal sistem: $($apps.Count). Minta AI membaca inventory.json, packages.txt, memory.txt, home.txt, webview.txt, dan capture-status.json."
    Write-Host 'Hasil disimpan lokal di output yang diabaikan Git. Jangan unggah dump mentah atau menganggapnya sebagai backup data aplikasi.'
} finally {
    ConvertTo-Json -InputObject @($script:captureResults) -Depth 5 | Set-Content -LiteralPath (Join-Path $OutputDirectory 'capture-status.json') -Encoding utf8
}

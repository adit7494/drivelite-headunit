$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$collector = Join-Path $projectRoot 'scripts\Inspect-Headunit.ps1'
$fixture = Join-Path $PSScriptRoot 'fixtures\mock-adb.ps1'
$testRoot = Join-Path $projectRoot ('build\collector-tests-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testRoot -Force | Out-Null
$previousScenario = $env:DRIVELITE_TEST_ADB_SCENARIO
$previousLog = $env:DRIVELITE_TEST_ADB_LOG
$env:DRIVELITE_TEST_ADB_LOG = Join-Path $testRoot 'commands.txt'
function Assert([bool]$Condition, [string]$Message) { if (-not $Condition) { throw $Message } }
function Expect-Failure([string]$Scenario) {
    $env:DRIVELITE_TEST_ADB_SCENARIO = $Scenario
    $destination = Join-Path $testRoot $Scenario
    $failed = $false
    try { & $collector -Serial 'fixture-device' -AdbPath $fixture -OutputDirectory $destination }
    catch { $failed = $true }
    Assert $failed "Scenario $Scenario should fail before publishing a usable inventory."
    Assert (-not (Test-Path -LiteralPath (Join-Path $destination 'inventory.json'))) 'Failed reads must not produce inventory.json.'
}
try {
    $env:DRIVELITE_TEST_ADB_SCENARIO = 'ok'
    $destination = Join-Path $testRoot 'ok'
    & $collector -Serial 'fixture-device' -Connect -AdbPath $fixture -OutputDirectory $destination
    $inventory = Get-Content (Join-Path $destination 'inventory.json') -Raw | ConvertFrom-Json
    Assert ($inventory.installedCount -eq 6 -and $inventory.knownCount -eq 7) 'Installed and retained/uninstalled packages must be distinguished, including the android framework package.'
    Assert (@($inventory.packages | Where-Object { $_.package -eq 'android' -and $_.installed -and $_.systemApp }).Count -eq 1) 'The core android package must not be omitted.'
    $disabled = $inventory.packages | Where-Object package -eq 'com.google.android.gms'
    $uninstalled = $inventory.packages | Where-Object package -eq 'com.android.egg'
    $thirdParty = $inventory.packages | Where-Object package -eq 'com.example.music'
    Assert ($disabled.installed -and $disabled.state -eq 'disabled') 'Already disabled packages must be preserved in the inventory.'
    Assert (-not $uninstalled.installed -and $uninstalled.state -eq 'not-installed' -and $uninstalled.systemApp) 'Known but uninstalled system apps must not count as installed.'
    Assert ($thirdParty.installed -and -not $thirdParty.systemApp -and $thirdParty.state -eq 'enabled') 'Third-party status must be factual, without an automatic removal decision.'
    Assert ($inventory.device.android -eq '8.1.0' -and $inventory.device.targetUser -eq 0) 'Actual device version and user must be recorded.'
    $animations = Get-Content (Join-Path $destination 'animations-before.json') -Raw | ConvertFrom-Json
    Assert ($animations.window_animation_scale -eq 'null') 'Unset animation overrides must remain distinguishable from 1.0.'
    'PASS: inventory separates installed, disabled, uninstalled, and system status.'
    Expect-Failure 'unauthorized'
    'PASS: unauthorized device cannot produce an inventory.'
    Expect-Failure 'required-failure'
    'PASS: failed package query cannot produce an inventory even with exit code 0.'
    $env:DRIVELITE_TEST_ADB_SCENARIO = 'optional-failure'
    $partial = Join-Path $testRoot 'optional-failure'
    & $collector -Serial 'fixture-device' -AdbPath $fixture -OutputDirectory $partial
    $status = @(Get-Content (Join-Path $partial 'capture-status.json') -Raw | ConvertFrom-Json)
    Assert (Test-Path -LiteralPath (Join-Path $partial 'inventory.json')) 'An optional failure should retain package inventory.'
    Assert (@($status | Where-Object { $_.file -eq 'webview.txt' -and -not $_.available }).Count -eq 1) 'Optional failure must be explicitly reported.'
    'PASS: unsupported optional query is reported without pretending it succeeded.'
    $commands = Get-Content -LiteralPath $env:DRIVELITE_TEST_ADB_LOG
    Assert (-not ($commands -match '\b(uninstall|install|disable-user|enable|default-state|setprop|reboot|root|remount)\b|settings (put|delete)')) 'Collector issued a mutating device command.'
    'PASS: all simulated device calls are read-only.'
} finally {
    $env:DRIVELITE_TEST_ADB_SCENARIO = $previousScenario
    $env:DRIVELITE_TEST_ADB_LOG = $previousLog
}

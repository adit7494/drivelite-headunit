[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory)][string]$Serial,
    [string]$ApkPath,
    [switch]$Connect,
    [switch]$GrantLocation,
    [switch]$SetAsHome
)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'Common.ps1')
$adb=Get-DriveLiteAdb $projectRoot
if(-not $ApkPath) {$ApkPath=Join-Path $projectRoot 'output\DriveLite.apk'}
$ApkPath=(Resolve-Path -LiteralPath $ApkPath).Path
if(-not $PSCmdlet.ShouldProcess($Serial,'Install DriveLite APK and optionally set HOME')) {return}
if($Connect) { & $adb connect $Serial; Assert-DriveLiteExit 'ADB connect' }
$state=(& $adb -s $Serial get-state 2>&1) -join ''
if($LASTEXITCODE -ne 0 -or $state.Trim() -ne 'device') {throw 'Perangkat belum authorized/device. Periksa adb devices -l dan dialog Allow di head unit.'}
$homeResult=& $adb -s $Serial shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
Assert-DriveLiteExit 'Read current HOME'
$previous=@($homeResult | Where-Object {$_ -match '^[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+$'}) | Select-Object -Last 1
if($previous -and $previous -notlike 'id.adhit.drivelite/*' -and $previous -notlike 'android/*') {
    $backupDir=Join-Path $projectRoot 'output\device-backups'
    New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
    $stamp=Get-Date -Format 'yyyyMMdd-HHmmss'
    @{serial=$Serial;component=$previous.Trim();createdUtc=(Get-Date).ToUniversalTime().ToString('o')} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $backupDir ($stamp+'-home.json')) -Encoding utf8
    Write-Host "HOME sebelumnya: $previous (tersimpan di output/device-backups)."
}
& $adb -s $Serial install -r $ApkPath
Assert-DriveLiteExit 'Install APK'
if($GrantLocation) { & $adb -s $Serial shell pm grant id.adhit.drivelite android.permission.ACCESS_FINE_LOCATION; Assert-DriveLiteExit 'Grant GPS permission' }
& $adb -s $Serial shell am start -W -n id.adhit.drivelite/.MainActivity
Assert-DriveLiteExit 'Open DriveLite'
if($SetAsHome) {
    & $adb -s $Serial shell cmd package set-home-activity --user 0 id.adhit.drivelite/.MainActivity
    Assert-DriveLiteExit 'Set HOME'
    & $adb -s $Serial shell input keyevent KEYCODE_HOME
    Assert-DriveLiteExit 'Open HOME'
} else { Write-Host 'Pratinjau terbuka. Uji dahulu; HOME default belum diubah oleh skrip ini.' }

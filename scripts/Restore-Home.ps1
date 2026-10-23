[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory)][string]$Serial,
    [string]$Component,
    [string]$BackupFile
)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'Common.ps1')
if($BackupFile) {
    $backup=Get-Content -LiteralPath $BackupFile -Raw | ConvertFrom-Json
    if($backup.serial -ne $Serial) {throw 'Serial backup berbeda dari perangkat tujuan.'}
    $Component=$backup.component
}
if(-not $Component -or $Component -notmatch '^[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+$') {throw 'Isi -Component dari HOME yang dicatat sebelumnya, atau gunakan -BackupFile.'}
$adb=Get-DriveLiteAdb $projectRoot
if(-not $PSCmdlet.ShouldProcess($Serial,"Restore HOME to $Component")) {return}
& $adb -s $Serial shell cmd package set-home-activity --user 0 $Component
Assert-DriveLiteExit 'Restore HOME'
& $adb -s $Serial shell input keyevent KEYCODE_HOME
Assert-DriveLiteExit 'Open restored HOME'
Write-Host 'HOME dipulihkan. DriveLite tetap terpasang.'

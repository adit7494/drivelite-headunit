[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'scripts\Common.ps1')
$chain=Get-DriveLiteToolchain $PSScriptRoot
$classes=Join-Path $PSScriptRoot 'build\classes'
if (-not (Test-Path -LiteralPath $classes)) { throw 'Jalankan .\build.ps1 dahulu.' }
$testDir=Clear-DriveLiteBuildDirectory (Join-Path $PSScriptRoot 'build') 'tests'
$classpath=$classes+';'+(Join-Path $chain.Platform 'android.jar')
& (Join-Path $chain.Jdk 'bin\javac.exe') -encoding UTF-8 --release 8 -cp $classpath -d $testDir (Join-Path $PSScriptRoot 'tests\LogicChecks.java')
Assert-DriveLiteExit 'Compile tests'
& (Join-Path $chain.Jdk 'bin\java.exe') -cp ($testDir+';'+$classpath) id.adhit.drivelite.LogicChecks
Assert-DriveLiteExit 'Logic checks'

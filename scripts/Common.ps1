$ErrorActionPreference = 'Stop'
function Get-DriveLiteToolsRoot([string]$ProjectRoot) {
    if ($env:DRIVELITE_TOOLS) { return [IO.Path]::GetFullPath($env:DRIVELITE_TOOLS) }
    $localRoot = Join-Path $ProjectRoot '.tools'
    if (Test-Path -LiteralPath $localRoot) { return $localRoot }
    $legacyRoot = Join-Path (Split-Path -Parent $ProjectRoot) 'tools'
    if (Test-Path -LiteralPath (Join-Path $legacyRoot 'jdk')) { return $legacyRoot }
    return $localRoot
}
function Find-DriveLiteToolDirectory([string]$Root, [string]$RelativeFile) {
    if (Test-Path -LiteralPath (Join-Path $Root $RelativeFile)) { return $Root }
    if (Test-Path -LiteralPath $Root) {
        foreach ($dir in Get-ChildItem -LiteralPath $Root -Directory) {
            if (Test-Path -LiteralPath (Join-Path $dir.FullName $RelativeFile)) { return $dir.FullName }
        }
    }
    throw "Tool tidak ditemukan di $Root. Jalankan .\setup.ps1 terlebih dahulu."
}
function Get-DriveLiteToolchain([string]$ProjectRoot) {
    $root = Get-DriveLiteToolsRoot $ProjectRoot
    [pscustomobject]@{
        Root = $root
        Jdk = Find-DriveLiteToolDirectory (Join-Path $root 'jdk') 'bin\javac.exe'
        BuildTools = Find-DriveLiteToolDirectory (Join-Path $root 'sdk-build-tools') 'aapt2.exe'
        Platform = Find-DriveLiteToolDirectory (Join-Path $root 'sdk-platform') 'android.jar'
    }
}
function Get-DriveLiteAdb([string]$ProjectRoot) {
    $localAdb = Join-Path (Get-DriveLiteToolsRoot $ProjectRoot) 'platform-tools\adb.exe'
    if (Test-Path -LiteralPath $localAdb) { return $localAdb }
    $found = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($found) { return $found.Source }
    throw 'ADB tidak ditemukan. Jalankan .\setup.ps1 -AdbOnly.'
}
function Assert-DriveLiteExit([string]$Stage) {
    if ($LASTEXITCODE -ne 0) { throw "$Stage gagal (exit $LASTEXITCODE)." }
}
function Clear-DriveLiteBuildDirectory([string]$BuildRoot, [string]$Name) {
    $root = [IO.Path]::GetFullPath($BuildRoot) + [IO.Path]::DirectorySeparatorChar
    $target = [IO.Path]::GetFullPath((Join-Path $BuildRoot $Name))
    if (-not $target.StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe build path.' }
    if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Recurse -Force }
    New-Item -ItemType Directory -Path $target -Force | Out-Null
    return $target
}

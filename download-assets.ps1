[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$credits=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'assets\credits.json') -Raw | ConvertFrom-Json
foreach($kind in @('covers','wallpapers')) {New-Item -ItemType Directory -Path (Join-Path $PSScriptRoot ('assets\'+$kind)) -Force | Out-Null}
Write-Host 'Artwork game milik penerbit masing-masing; sumber dan checksum ada di assets/credits.json.'
foreach($wallpaper in $credits.wallpapers) {
    Invoke-WebRequest -Uri $wallpaper.url -OutFile (Join-Path $PSScriptRoot ('assets\wallpapers\'+$wallpaper.file))
}
foreach($cover in $credits.covers) {
    Invoke-WebRequest -Uri $cover.coverUrl -OutFile (Join-Path $PSScriptRoot ('assets\covers\'+$cover.key+'.jpg'))
}
foreach($expected in $credits.sha256) {
    $path=Join-Path $PSScriptRoot ('assets\'+$expected.file)
    if((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -ne $expected.sha256) {throw ('Artwork berubah di sumber: '+$expected.file+'. Periksa sumber sebelum memperbarui checksum.')}
}
Write-Host '50 file artwork terunduh dan checksum sesuai. Jalankan .\build.ps1 -Artwork Original.'
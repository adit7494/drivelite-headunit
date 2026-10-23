[CmdletBinding()]
param([Parameter(Mandatory)][string]$Destination)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot = Split-Path -Parent $PSScriptRoot
$catalog = Get-Content -LiteralPath (Join-Path $projectRoot 'assets\credits.json') -Raw | ConvertFrom-Json
New-Item -ItemType Directory -Path (Join-Path $Destination 'covers'), (Join-Path $Destination 'wallpapers') -Force | Out-Null
$palette = @('#437ACC','#9D5ED4','#3D9C91','#D47E53','#708CDE','#A25A7D','#688A55','#9B804A')
for ($i=0; $i -lt $catalog.covers.Count; $i++) {
    $game = $catalog.covers[$i]
    foreach ($kind in @('covers','wallpapers')) {
        $w = if ($kind -eq 'covers') {300} else {1024}
        $h = if ($kind -eq 'covers') {450} else {600}
        $bitmap = [Drawing.Bitmap]::new($w,$h)
        $graphics = [Drawing.Graphics]::FromImage($bitmap)
        $graphics.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $accent = [Drawing.ColorTranslator]::FromHtml($palette[$i % $palette.Count])
        $gradient = [Drawing.Drawing2D.LinearGradientBrush]::new([Drawing.Rectangle]::new(0,0,$w,$h),[Drawing.Color]::FromArgb(9,17,31),$accent,35.0)
        $graphics.FillRectangle($gradient,0,0,$w,$h)
        $pen = [Drawing.Pen]::new([Drawing.Color]::FromArgb(70,255,255,255),2)
        for ($ring=0; $ring -lt 6; $ring++) {
            $size = [int]($w*.55+$ring*$w*.16)
            $graphics.DrawEllipse($pen,[int]($w*.72-$size/2),[int]($h*.45-$size/2),$size,$size)
        }
        $large = [Drawing.Font]::new('Segoe UI',($w*.14),[Drawing.FontStyle]::Regular,[Drawing.GraphicsUnit]::Pixel)
        $small = [Drawing.Font]::new('Segoe UI',($w*.035),[Drawing.FontStyle]::Regular,[Drawing.GraphicsUnit]::Pixel)
        $graphics.DrawString(('{0:D2}' -f ($i+1)),$large,[Drawing.Brushes]::White,[single]($w*.09),[single]($h*.45))
        $graphics.DrawString('DRIVELITE / DEMO',$small,[Drawing.Brushes]::White,[single]($w*.09),[single]($h*.82))
        $graphics.DrawString('Original geometric artwork',$small,[Drawing.Brushes]::White,[single]($w*.09),[single]($h*.87))
        $bitmap.Save((Join-Path $Destination ($kind+'\'+$game.key+'.jpg')),[Drawing.Imaging.ImageFormat]::Jpeg)
        $small.Dispose(); $large.Dispose(); $pen.Dispose(); $gradient.Dispose(); $graphics.Dispose(); $bitmap.Dispose()
    }
}
@{mode='demo';description='Original procedural geometric artwork. Game images are not bundled in this build.'} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $Destination 'demo-artwork.json') -Encoding utf8

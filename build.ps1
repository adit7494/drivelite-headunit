[CmdletBinding()]
param(
    [ValidateSet('Auto','Demo','Original')][string]$Artwork='Auto',
    [string]$OutputDirectory,
    [string]$KeystorePath
)
$ErrorActionPreference='Stop'
$projectDir=$PSScriptRoot
. (Join-Path $projectDir 'scripts\Common.ps1')
$chain=Get-DriveLiteToolchain $projectDir
$javaExe=Join-Path $chain.Jdk 'bin\java.exe'
$androidJar=Join-Path $chain.Platform 'android.jar'
$buildDir=Join-Path $projectDir 'build'
foreach($dir in @('generated','classes','dex','assets')) { $null=Clear-DriveLiteBuildDirectory $buildDir $dir }
if(-not $OutputDirectory) { $OutputDirectory=Join-Path $projectDir 'output' }
$OutputDirectory=[IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$assetStage=Join-Path $buildDir 'assets'
Copy-Item -LiteralPath (Join-Path $projectDir 'assets\credits.json') -Destination $assetStage
$catalog=Get-Content -LiteralPath (Join-Path $projectDir 'assets\credits.json') -Raw | ConvertFrom-Json
$originalReady=$true
foreach($game in $catalog.covers) {
    foreach($kind in @('covers','wallpapers')) {
        if(-not (Test-Path -LiteralPath (Join-Path $projectDir ('assets\'+$kind+'\'+$game.key+'.jpg')))) {$originalReady=$false}
    }
}
if($Artwork -eq 'Auto') { $Artwork=if($originalReady) {'Original'} else {'Demo'} }
if($Artwork -eq 'Original') {
    if(-not $originalReady) { throw 'Artwork belum lengkap. Jalankan .\download-assets.ps1, atau build dengan -Artwork Demo.' }
    Copy-Item -LiteralPath (Join-Path $projectDir 'assets\covers'),(Join-Path $projectDir 'assets\wallpapers') -Destination $assetStage -Recurse
} else {
    & (Join-Path $projectDir 'scripts\New-DemoArtwork.ps1') -Destination $assetStage
}
Write-Host "Artwork mode: $Artwork"
& (Join-Path $chain.BuildTools 'aapt2.exe') compile --dir (Join-Path $projectDir 'res') -o (Join-Path $buildDir 'resources.zip')
Assert-DriveLiteExit 'aapt2 compile'
$unsigned=Join-Path $buildDir 'unsigned.apk'
& (Join-Path $chain.BuildTools 'aapt2.exe') link -I $androidJar --manifest (Join-Path $projectDir 'AndroidManifest.xml') --java (Join-Path $buildDir 'generated') -A $assetStage --min-sdk-version 21 --target-sdk-version 28 -o $unsigned (Join-Path $buildDir 'resources.zip')
Assert-DriveLiteExit 'aapt2 link'
# Windows aapt2 may put backslashes in ZIP paths. Android AssetManager expects '/'.
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::Open($unsigned,[IO.Compression.ZipArchiveMode]::Update)
try {
    foreach($entry in @($zip.Entries | Where-Object {$_.FullName.Contains('\')})) {
        $name=$entry.FullName.Replace('\','/')
        $buffer=[IO.MemoryStream]::new(); $inputStream=$entry.Open()
        try {$inputStream.CopyTo($buffer)} finally {$inputStream.Dispose()}
        $entry.Delete()
        $next=$zip.CreateEntry($name,[IO.Compression.CompressionLevel]::Optimal)
        $outputStream=$next.Open()
        try {$buffer.Position=0; $buffer.CopyTo($outputStream)} finally {$outputStream.Dispose(); $buffer.Dispose()}
    }
} finally {$zip.Dispose()}
$sources=@(Get-ChildItem -LiteralPath (Join-Path $projectDir 'src'),(Join-Path $buildDir 'generated') -Recurse -Filter '*.java' | ForEach-Object {$_.FullName})
& (Join-Path $chain.Jdk 'bin\javac.exe') -encoding UTF-8 --release 8 -classpath $androidJar -d (Join-Path $buildDir 'classes') @sources
Assert-DriveLiteExit 'javac'
& (Join-Path $chain.Jdk 'bin\jar.exe') cf (Join-Path $buildDir 'classes.jar') -C (Join-Path $buildDir 'classes') .
Assert-DriveLiteExit 'jar'
& $javaExe -cp (Join-Path $chain.BuildTools 'lib\d8.jar') com.android.tools.r8.D8 --lib $androidJar --min-api 21 --output (Join-Path $buildDir 'dex') (Join-Path $buildDir 'classes.jar')
Assert-DriveLiteExit 'd8'
Push-Location -LiteralPath (Join-Path $buildDir 'dex')
try { & (Join-Path $chain.BuildTools 'aapt.exe') add $unsigned classes.dex; Assert-DriveLiteExit 'aapt add dex' } finally {Pop-Location}
$aligned=Join-Path $buildDir 'aligned.apk'
& (Join-Path $chain.BuildTools 'zipalign.exe') -f 4 $unsigned $aligned
Assert-DriveLiteExit 'zipalign'
if(-not $KeystorePath) { $KeystorePath=if($env:DRIVELITE_KEYSTORE) {$env:DRIVELITE_KEYSTORE} else {Join-Path $projectDir 'signing\drivelite-local.jks'} }
$KeystorePath=[IO.Path]::GetFullPath($KeystorePath)
New-Item -ItemType Directory -Path (Split-Path -Parent $KeystorePath) -Force | Out-Null
$alias=if($env:DRIVELITE_KEY_ALIAS) {$env:DRIVELITE_KEY_ALIAS} else {'drivelite'}
# 'android' is a local development default, not a production signing credential.
$env:DRIVELITE_BUILD_STOREPASS=if($env:DRIVELITE_STORE_PASSWORD) {$env:DRIVELITE_STORE_PASSWORD} else {'android'}
$env:DRIVELITE_BUILD_KEYPASS=if($env:DRIVELITE_KEY_PASSWORD) {$env:DRIVELITE_KEY_PASSWORD} else {$env:DRIVELITE_BUILD_STOREPASS}
try {
    if(-not (Test-Path -LiteralPath $KeystorePath)) {
        & (Join-Path $chain.Jdk 'bin\keytool.exe') -genkeypair -keystore $KeystorePath -storetype JKS -storepass:env DRIVELITE_BUILD_STOREPASS -keypass:env DRIVELITE_BUILD_KEYPASS -alias $alias -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=DriveLite Local, O=Personal Headunit'
        Assert-DriveLiteExit 'keytool'
    }
    $finalApk=Join-Path $OutputDirectory 'DriveLite.apk'
    & $javaExe -jar (Join-Path $chain.BuildTools 'lib\apksigner.jar') sign --ks $KeystorePath --ks-key-alias $alias --ks-pass env:DRIVELITE_BUILD_STOREPASS --key-pass env:DRIVELITE_BUILD_KEYPASS --min-sdk-version 21 --v1-signing-enabled true --v2-signing-enabled true --out $finalApk $aligned
    Assert-DriveLiteExit 'apksigner sign'
} finally { Remove-Item Env:\DRIVELITE_BUILD_STOREPASS,Env:\DRIVELITE_BUILD_KEYPASS -ErrorAction SilentlyContinue }
& $javaExe -jar (Join-Path $chain.BuildTools 'lib\apksigner.jar') verify --verbose $finalApk
Assert-DriveLiteExit 'apksigner verify'
& (Join-Path $chain.BuildTools 'zipalign.exe') -c 4 $finalApk
Assert-DriveLiteExit 'zipalign verify'
$hash=(Get-FileHash -LiteralPath $finalApk -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText((Join-Path $OutputDirectory 'SHA256SUMS.txt'),$hash+'  DriveLite.apk'+[Environment]::NewLine,[Text.UTF8Encoding]::new($false))
Write-Host "APK: $finalApk"
Write-Host "SHA256: $hash"
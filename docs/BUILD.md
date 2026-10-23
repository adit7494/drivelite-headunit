# Build dari source di Windows

Build tidak memerlukan Android Studio atau Gradle. Skrip memakai PowerShell, JDK 17, Android Build-Tools 35.0.1, dan platform API 28. Toolchain disimpan lokal di `.tools/`, dengan versi dan SHA-256 di [config/toolchain.json](../config/toolchain.json).

## 1. Ambil source

```powershell
git clone https://github.com/adit7494/drivelite-headunit.git
cd drivelite-headunit
```

Tanpa Git, gunakan **Code → Download ZIP**, ekstrak, lalu buka PowerShell di folder yang berisi `build.ps1`.

## 2. Siapkan toolchain

```powershell
.\setup.ps1
```

Unduhan awal sekitar 330 MB dan membutuhkan koneksi internet. Skrip memeriksa SHA-256 setiap arsip sebelum ekstraksi. Paket tetap mengikuti lisensi [Android SDK](https://developer.android.com/studio/terms) dan [Temurin](https://adoptium.net/about). Skrip tidak mengubah PATH sistem.

Jika skrip diblokir, jalankan `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass` untuk terminal yang sedang digunakan, lalu ulangi.

## 3. Build demo yang dapat dibagikan

```powershell
.\build.ps1 -Artwork Demo
.\test.ps1
```

Hasil:

```text
output/
  DriveLite.apk
  SHA256SUMS.txt
```

Build demo menghasilkan 25 cover dan wallpaper geometris melalui kode di `scripts/New-DemoArtwork.ps1`. Tidak menggunakan gambar game pihak ketiga. APK tetap memiliki fitur launcher, GPS, jam, favorit, dan menu tekan tahan.

## Artwork game opsional

Untuk instalasi pribadi dengan artwork seperti screenshot:

```powershell
.\download-assets.ps1
.\build.ps1 -Artwork Original
.\test.ps1
```

Skrip mengunduh 25 cover dan 25 wallpaper dari URL di `assets/credits.json`, lalu memeriksa checksum. File gambar tersimpan lokal dan tidak ikut Git. Jika checksum berubah, periksa sumber dahulu; jangan mengabaikan hasilnya. Hak artwork tetap milik penerbit dan tidak termasuk lisensi MIT proyek ini.

`-Artwork Auto` adalah default: memakai artwork asli jika seluruh 50 file tersedia; jika belum lengkap, memakai demo. Untuk hasil yang dapat diprediksi, tentukan `Demo` atau `Original` secara eksplisit.

## Kunci pembaruan APK

Build pertama membuat `signing/drivelite-local.jks` bila belum ada. Simpan kunci ini di tempat pribadi. Build berikutnya harus memakai kunci sama agar dapat memperbarui aplikasi tanpa uninstall dan kehilangan data favorit.

Default kata sandi `android` hanya untuk pengembangan lokal. Untuk kunci milik sendiri:

```powershell
$env:DRIVELITE_KEYSTORE = 'D:\Private\drivelite.jks'
$env:DRIVELITE_KEY_ALIAS = 'drivelite'
# Isi DRIVELITE_STORE_PASSWORD dan DRIVELITE_KEY_PASSWORD melalui lingkungan privat Anda.
.\build.ps1 -Artwork Demo
```

Jangan commit kunci, password, atau token. Variabel password tidak ditulis ke log build. Jika berganti dari APK Releases ke build dengan kunci lain, Android menolak pembaruan; baca [signature mismatch](TROUBLESHOOTING.md#install_failed_update_incompatible).

## Memahami hasil pengujian

`test.ps1` menjalankan 14 pemeriksaan logika: pembagian cover, stabilitas mapping saat daftar berubah, dan filter drift GPS. Ini tidak menggantikan pengujian di head unit. CI juga membangun APK, memverifikasi tanda tangan/zip alignment, dan menjalankan pemeriksaan yang sama.

Artefak CI menggunakan kunci yang dibuat pada runner dan ditujukan untuk pengujian. Gunakan APK dari Releases atau kunci build Anda sendiri yang dipertahankan untuk jalur pembaruan tetap.

## Struktur source

```text
AndroidManifest.xml
src/id/adhit/drivelite/
  MainActivity.java      UI, daftar aplikasi, favorit, clock, navigasi
  Artwork.java           thumbnail, decode worker, crossfade
  CoverAssignments.java  pembagian cover yang stabil
  SpeedMonitor.java      GPS, data kedaluwarsa, filter 1–2 km/h
res/                     tema dan ikon aplikasi
assets/credits.json      katalog dan sumber gambar opsional
scripts/                 demo artwork, ADB install dan restore
tests/LogicChecks.java   pemeriksaan logika
```

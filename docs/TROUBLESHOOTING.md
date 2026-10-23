# Troubleshooting

## ADB tidak dikenali

Gunakan path lengkap setelah `.\setup.ps1 -AdbOnly`:

```powershell
& '.\.tools\platform-tools\adb.exe' version
```

Setup tidak mengubah PATH sistem. Jika folder `.tools` belum ada, jalankan setup dari folder proyek.

## `unauthorized`

Buka layar head unit dan setujui dialog Allow USB debugging. Jalankan `adb devices -l` lagi. Jika dialog tidak muncul, gunakan menu revoke USB debugging authorizations pada perangkat, lalu sambungkan ulang.

## Timeout atau connection refused

- Periksa IP terbaru pada detail Wi-Fi head unit; contoh dokumentasi bukan alamat perangkat Anda.
- Pastikan PC dan head unit berada pada jaringan sama dan tidak terkena client/AP isolation.
- USB debugging yang aktif tidak selalu berarti ADB Wi-Fi juga aktif.
- Pada Android 11+, port pairing bukan port koneksi. Ikuti angka yang ditampilkan pada layar.
- Pada TS7 lama, `persist.wifi.debug` hanya berlaku jika firmware mendukung. Reboot dapat memerlukan pengaktifan ulang.

## `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

APK baru memakai kunci penandatanganan berbeda. Pakai kembali keystore yang menandatangani versi terpasang. APK CI dibuat dengan kunci runner dan tidak dijamin dapat memperbarui APK Releases.

Jika memilih uninstall lalu install ulang dengan kunci lain, catat favorit dahulu dan pastikan launcher lain masih berfungsi; data DriveLite akan hilang.

## GPS merah / angka `—`

Berikan izin lokasi, aktifkan GPS, dan tunggu fix pada tempat dengan sinyal memadai. Data yang sudah kedaluwarsa atau kualitasnya buruk tetap ditampilkan sebagai tidak tersedia. Fake/mock location sengaja tidak dipakai.

## Kecepatan diam masih 1–2 km/h pada aplikasi lain

DriveLite menekan nilai tersebut menjadi 0 hanya pada tampilannya sendiri. Filter tidak mengubah GPS Android atau aplikasi navigasi lain.

## Cover demo, bukan game

Ini perilaku APK publik. Screenshot memakai artwork opsional. Ikuti [build artwork game](BUILD.md#artwork-game-opsional) untuk instalasi pribadi, atau gunakan gambar milik Anda sendiri dengan nama file sesuai katalog.

## `Checksum tidak sesuai`

Jangan melanjutkan dengan arsip itu. Untuk toolchain, hapus arsip unduhan yang disebut lalu ulangi setup. Untuk artwork, sumber mungkin berubah; periksa URL dan gambar sebelum memperbarui katalog checksum.

## Launcher tidak tampil baik pada ROM lain

Proyek diuji pada TS7 Android 8.1/API 27, 1024 × 600. Gunakan pratinjau sebelum menjadikan HOME default. Kembalikan launcher lama lewat [pemulihan](RECOVERY.md), kemudian buat issue dengan versi Android sebenarnya, resolusi, screenshot tanpa data pribadi, dan langkah reproduksi. Jangan unggah dump lokasi, token, atau keystore.

# Catatan debloat TS7 pengembang

[Panduan debloat langkah demi langkah](DEBLOAT.md) · [Kembali ke README](../README.md)

Ringkasan ini disusun dari inventaris, metadata paket, log tindakan, dan snapshot memori lokal tanggal **22 Oktober 2026**. Log mentah, identitas perangkat, serta salinan APK audit tidak diunggah. Ini hasil satu perangkat, bukan profil yang otomatis diterapkan pada semua TS7.

## Perangkat dan urutan perubahan

Android sebenarnya **8.1/API 27**, ARMv7, layar 1024 × 600. Android melihat RAM sekitar **1 GiB**, berbeda dari angka 2 GB pada menu About. Debloat dilakukan sebelum launcher DriveLite dipasang.

| Tahap | Perubahan yang tercatat |
| --- | --- |
| Awal | 90 paket terpasang untuk user 0. Play Store, Play Services, dan Google WebView sudah nonaktif. Chrome adalah WebView aktif. |
| Debloat aplikasi opsional | Sembilan paket pada [tabel panduan](DEBLOAT.md#3-pilih-aplikasi-yang-tidak-digunakan) dilepas dengan `pm uninstall -k --user 0`. Data diminta tetap disimpan. |
| Temuan keamanan | `com.tw.jar1` dihentikan dan di-uninstall untuk user 0 tanpa mempertahankan datanya. `com.tw.core` dinonaktifkan. |
| Sesudah tahap awal | 80 paket terpasang; 22 paket dalam daftar perlindungan tetap terpasang/enabled. HOME masih launcher DoFun dan WebView masih Chrome. |
| Optimasi berikutnya | Tiga skala animasi diubah menjadi 0,5. FOTA `com.abupdate.fota_demo_iot` dan Carrier Services `com.google.android.ims` dinonaktifkan. Jumlah paket terpasang tetap 80. |
| Launcher | DriveLite kemudian dipasang sebagai HOME. Launcher OEM tetap disimpan untuk pemulihan. |

FOTA dinonaktifkan dengan konsekuensi update terkait berhenti. Carrier Services dinonaktifkan pada unit tanpa SIM; ini tidak dijadikan rekomendasi umum untuk unit dengan layanan operator.

## Snapshot memori

| Ukuran | Sebelum | Sesudah debloat awal | Sesudah optimasi lanjutan |
| --- | ---: | ---: | ---: |
| Used RAM dari `dumpsys meminfo` | 685,3 MiB | 542,7 MiB | 489,9 MiB |
| ZRAM fisik terpakai | 214,2 MiB | 137,5 MiB | Tidak dibandingkan |

Selisih Used RAM tahap awal sekitar **142,7 MiB**. Ini tiga snapshot pada waktu berbeda, bukan benchmark terkontrol atau jaminan penghematan pada perangkat lain. Paket yang tidak aktif sebelumnya tidak dihitung sebagai hasil baru. Pengguna melaporkan menu terasa lebih ringan setelah debloat, tetapi tidak ada klaim kenaikan FPS dari data tersebut.

Percobaan kompilasi `cmd package compile -m speed -f` pada launcher OEM dan Settings mengembalikan kegagalan. Karena itu tidak dimasukkan sebagai langkah optimasi yang berhasil.

## Temuan JarService dan TWCore

APK lokal `com.tw.jar1` V1.12 memiliki MD5 `6c2e34b30da42085240ede53ab6107d4`, cocok dengan indikator JarService yang dipublikasikan [Kaspersky Securelist](https://securelist.com/android-head-unit-malware/121106/). MD5 TWCore lokal `ea24487996eb70c1780922fb3063bcc5` juga tercantum pada laporan tersebut sebagai updater sah yang digunakan dalam rantai distribusi. TWCore tidak disamakan dengan semua layanan mobil `com.tw.*`.

Tindakan pada unit ini didasarkan pada pencocokan APK, bukan nama paket saja. JarService tidak termasuk daftar aplikasi untuk dipulihkan. Penanganan paket ini tidak membuktikan seluruh firmware bebas dari payload lain. Perangkat lain memerlukan pemeriksaan sendiri; jangan mengunduh atau memasang sampel APK audit.

## Verifikasi dan batasnya

Pemeriksaan ADB mengonfirmasi status paket, proses target, HOME, provider WebView, serta tetap tersedianya paket yang dilindungi. Radio/audio, panggilan Bluetooth, kamera mundur, tombol setir, GPS, dan boot/sleep-wake memerlukan pengujian langsung pada kendaraan; status paket enabled saja tidak membuktikan seluruh fungsi tersebut bekerja.

Untuk mengulang metode pada perangkat sendiri, ikuti [panduan debloat](DEBLOAT.md) dan simpan kondisi awal. Jangan menyalin seluruh riwayat perubahan sebagai perintah massal.

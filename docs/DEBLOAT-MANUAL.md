# Referensi perintah debloat ADB

[Mulai dengan AI](DEBLOAT.md) · [Hasil pada TS7 pengembang](DEBLOAT-TS7.md) · [Setup/koneksi ADB](INSTALL.md#3b-cara-adb-pc-windows)

Dokumen ini referensi teknis bagi AI atau pengguna yang ingin melihat perintah rinci. Untuk mulai tanpa mengetik rangkaian perintah PC sendiri, ikuti [setup dengan AI](DEBLOAT.md).

Debloat mengurangi aplikasi dan proses yang tidak dipakai. **Memasang APK DriveLite tidak menjalankan debloat.** Panduan ini dapat diikuti tanpa mengganti launcher.

Daftar berikut berasal dari TS7 Android 8.1/API 27 yang diperiksa pengembang. Nama paket dan ketergantungan berbeda antar-ROM; pilih hanya fitur yang memang tidak digunakan. Mulai dengan **menonaktifkan satu paket**, uji fungsi mobil, lalu lanjutkan. Lakukan pekerjaan saat kendaraan terparkir dan daya head unit stabil.

## 1. Hubungkan PC dan pilih perangkat

Unduh/clone repository dan buka PowerShell di folder proyek. Jika skrip diblokir, jalankan `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass` untuk sesi ini.

```powershell
.\setup.ps1 -AdbOnly
$adb = '.\.tools\platform-tools\adb.exe'
$serial = '192.168.1.50:5555' # CONTOH: ganti dengan alamat head unit Anda.
& $adb connect $serial
& $adb devices -l
& $adb -s $serial get-state
```

Hasil `get-state` harus `device`. Jika `unauthorized`, setujui dialog pada head unit; jika gagal terhubung, ikuti [metode Wi-Fi/USB sesuai versi Android](INSTALL.md#sambungkan-head-unit). USB debugging saja belum tentu membuka port Wi-Fi. Untuk USB, isi `$serial` dengan serial dari `adb devices -l` dan lewati `connect`.

Periksa sistem dan user:

```powershell
& $adb -s $serial shell getprop ro.build.version.release
& $adb -s $serial shell getprop ro.build.version.sdk
& $adb -s $serial shell am get-current-user
& $adb -s $serial shell pm list users
```

Perintah perubahan dalam panduan ini khusus **user 0**. Lanjutkan hanya jika itu pengguna utama yang ingin diubah. Simpan sesi PowerShell ini agar variabel `$adb`, `$serial`, dan `$snapshot` tetap tersedia.

## 2. Simpan kondisi sebelum perubahan

```powershell
$snapshot = Join-Path 'output\debloat' (Get-Date -Format 'yyyyMMdd-HHmmss')
New-Item -ItemType Directory -Path $snapshot -Force | Out-Null
& $adb -s $serial shell pm list packages --user 0 | Set-Content "$snapshot\installed-before.txt"
& $adb -s $serial shell pm list packages -u -f --user 0 | Set-Content "$snapshot\all-known-before.txt"
& $adb -s $serial shell pm list packages -d --user 0 | Set-Content "$snapshot\disabled-before.txt"
& $adb -s $serial shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | Set-Content "$snapshot\home-before.txt"
& $adb -s $serial shell dumpsys webviewupdate | Set-Content "$snapshot\webview-before.txt"
& $adb -s $serial shell dumpsys meminfo | Set-Content "$snapshot\memory-before.txt"
$animationBefore = @{}
foreach ($key in @('window_animation_scale', 'transition_animation_scale', 'animator_duration_scale')) {
    $animationBefore[$key] = ((& $adb -s $serial shell settings get global $key) -join '').Trim()
}
$animationBefore | ConvertTo-Json | Set-Content "$snapshot\animations-before.json"
```

Buka file hasilnya dan pastikan berisi data, bukan pesan koneksi gagal. Ini **catatan status**, bukan backup data aplikasi atau firmware. Folder `output/` diabaikan Git; simpan salinannya sendiri dan periksa data pribadi sebelum membagikan log.

## 3. Pilih aplikasi yang tidak digunakan

| Paket pada TS7 pengembang | Fitur yang berhenti jika dinonaktifkan | Prioritas |
| --- | --- | --- |
| `com.google.android.googlequicksearchbox` | Google Search, Assistant, dan interaksi suara yang bergantung padanya | Jika tidak memakai Assistant; prosesnya menonjol pada audit awal |
| `com.dofun.dofunweather.main` | Cuaca/widget DoFun tidak diperbarui | Jika tidak memakai cuaca bawaan |
| `com.android.wallpaperbackup` | Backup/pemulihan wallpaper | Jika tidak memakai backup wallpaper |
| `com.google.android.backuptransport` | Backup/pemulihan Google terkait | Jika tidak memakai backup Google |
| `com.google.android.syncadapters.contacts` | Sinkronisasi kontak Google | Jika tidak memakai kontak Google; pertahankan provider kontak Android |
| `com.android.egg` | Easter egg Android | Manfaat performa biasanya kecil |
| `com.android.dreams.basic` | Screensaver dasar | Jika tidak memakai screensaver |
| `com.android.providers.partnerbookmarks` | Bookmark bawaan mitra | Jika tidak memakai integrasi bookmark terkait |
| `com.android.bookmarkprovider` | Penyedia bookmark bawaan | Jika tidak memakai integrasi bookmark terkait |

Tidak semua paket akan ada pada perangkat lain. Paket yang hilang dilewati; nama yang mirip tidak berarti fungsi atau tingkat risikonya sama.

## 4. Nonaktifkan satu paket, lalu uji

Contoh berikut memilih Google Search/Assistant. Ganti `$pkg` dengan **satu nama persis** dari tabel setelah membaca dampaknya.

```powershell
$pkg = 'com.google.android.googlequicksearchbox'
& $adb -s $serial shell pm list packages --user 0 $pkg
& $adb -s $serial shell dumpsys package $pkg | Set-Content "$snapshot\$pkg-before.txt"
```

Pastikan hasil inventaris benar-benar `package:` diikuti nama yang dipilih. Pada metadata, catat baris `User 0:`: `installed` dan `enabled`. Nilai `enabled=0` berarti mengikuti default paket, `1` diaktifkan eksplisit, `2/3/4` menunjukkan bentuk status nonaktif. Jika paket sudah nonaktif, lewati; jangan menganggapnya sebagai hasil debloat baru.

```powershell
& $adb -s $serial shell pm disable-user --user 0 $pkg
& $adb -s $serial shell pm list packages -d --user 0 $pkg
& $adb -s $serial shell dumpsys package $pkg | Set-Content "$snapshot\$pkg-after.txt"
```

Contoh bentuk hasil yang diharapkan, bukan screenshot sesi perangkat Anda:

```text
Package com.google.android.googlequicksearchbox new state: disabled-user
package:com.google.android.googlequicksearchbox
```

Jika muncul `SecurityException`, `Unknown package`, atau status tidak berubah, hentikan langkah itu. Jangan memaksa root atau menghapus file sistem.

Uji radio, musik/panggilan Bluetooth, kamera mundur, tombol setir, pengaturan mobil, GPS, dan Home. Jika fungsi yang diperlukan terganggu, [pulihkan paket](#8-pemulihan) sebelum memilih paket berikutnya. Sesudah pengujian awal, uji juga boot dan sleep/wake dengan prosedur kendaraan Anda.

## 5. Opsional: hapus untuk user 0 setelah pengujian

Menonaktifkan sudah cukup untuk menghentikan komponen paket. Uninstall untuk user tidak menambah RAM fisik atau otomatis mengosongkan partisi sistem.

Pemulihan dengan `install-existing` memerlukan APK yang masih dikenal sistem. Untuk aplikasi biasa yang hanya berada di partisi data, uninstall bisa menghapus APK sehingga diperlukan installer asli. **Untuk panduan ini, gunakan langkah uninstall hanya pada paket sistem yang sudah diverifikasi.**

```powershell
& $adb -s $serial shell pm list packages -s --user 0 $pkg
& $adb -s $serial shell pm path $pkg
```

Perintah pertama harus memuat nama paket persis yang dipilih. Pada ROM yang mendukung pemulihan paket sistem dan setelah fungsi mobil lulus pengujian:

```powershell
& $adb -s $serial shell pm uninstall -k --user 0 $pkg
& $adb -s $serial shell pm list packages --user 0 $pkg
& $adb -s $serial shell pm list packages -u --user 0 $pkg
```

Harapkan `Success`, paket tidak lagi ada pada daftar terpasang user 0, dan masih dikenal pada daftar `-u`. `-k` meminta data/cache dipertahankan; itu bukan pengganti backup. Metode `cmd package install-existing` telah berhasil diuji pada TS7 pengembang. Jika kemampuan pemulihan ROM Anda belum diketahui, tetap gunakan **disable-user**.

## 6. Optimasi lanjutan yang bersifat pilihan

### Animasi 0,5x

Sesudah menyimpan `animations-before.json` pada langkah 2:

```powershell
& $adb -s $serial shell settings put global window_animation_scale 0.5
& $adb -s $serial shell settings put global transition_animation_scale 0.5
& $adb -s $serial shell settings put global animator_duration_scale 0.5
```

Transisi lebih singkat dapat membuat menu terasa lebih cepat; ini bukan peningkatan kemampuan CPU/GPU atau jaminan FPS. [Android menjelaskan efek skala animasi di Developer options](https://developer.android.com/studio/debug/dev-options#drawing).

### FOTA dan Carrier Services

| Paket | Pertimbangan sebelum memilih |
| --- | --- |
| `com.abupdate.fota_demo_iot` | Komponen update firmware. Menonaktifkannya memutus jalur update yang bergantung padanya. Siapkan cara mengaktifkannya kembali sebelum update resmi. |
| `com.google.android.ims` | Carrier Services. Pertimbangkan hanya jika dukungan operator/RCS tidak digunakan; unit pengembang tidak memakai SIM. Jangan menerapkannya sebagai daftar umum untuk head unit dengan SIM. |

Gunakan prosedur satu paket pada langkah 4 dan simpan metadata awal. Kedua paket ini **dinonaktifkan**, bukan di-uninstall, pada perangkat pengembang. TWCore dan JarService dibahas terpisah dalam [catatan audit TS7](DEBLOAT-TS7.md#temuan-jarservice-dan-twcore).

## 7. Paket yang dipertahankan

Pada ROM TS7 ini, pertahankan:

- Layanan mobil: `com.tw.service`, `com.tw.service.xt`, `com.tw.carinfoservice`, `com.dofun.carsetting`.
- Fungsi kendaraan/audio: `com.tw.radio`, `com.tw.bt`, `com.tw.eq`, `com.tw.media`, `com.tw.auxin`, `com.tw.reverse`, `com.tw.keypad`, `com.tw.carchoose`, `com.tw.bootanimation`.
- Launcher OEM `com.dofun.variety`, termasuk setelah DriveLite dipasang, untuk pemulihan.
- Settings, SystemUI, keyboard, PackageInstaller, provider kontak/media/unduhan/penyimpanan, Bluetooth, lokasi, serta layanan telepon/modem dan framework.
- Provider WebView aktif. Pada unit pengembang ini adalah **Chrome**, sehingga Chrome dipertahankan. Periksa `webview-before.txt` perangkat Anda.
- Terminal `jackpal.androidterm` selama dipakai mengaktifkan ADB Wi-Fi, alat diagnostik vendor, dan paket yang ketergantungannya belum diketahui.

Play Store, Play Services, dan Google WebView sudah nonaktif **sebelum** audit perangkat pengembang; itu bukan rekomendasi untuk menonaktifkannya pada unit lain. Navigasi, login, dan aplikasi lain dapat bergantung pada layanan Google. Jangan menonaktifkan seluruh paket `com.tw.*` atau `com.google.*` sekaligus.

## 8. Pemulihan

### Paket yang hanya dinonaktifkan

Gunakan `$pkg` untuk satu paket yang dipulihkan. Jika metadata **sebelum** perubahan menunjukkan `enabled=0`, kembalikan ke default paket:

```powershell
& $adb -s $serial shell pm default-state --user 0 $pkg
```

Jika semula `enabled=1`, gunakan perintah ini sebagai gantinya:

```powershell
& $adb -s $serial shell pm enable --user 0 $pkg
```

Pilih satu perintah yang sesuai, bukan menjalankan keduanya. Jika `default-state` tidak disediakan ROM, `enable` dapat mengaktifkan aplikasi tetapi statusnya menjadi aktif eksplisit. Jangan mengaktifkan paket yang memang sudah nonaktif sebelum debloat.

### Paket sistem yang di-uninstall untuk user 0

```powershell
& $adb -s $serial shell cmd package install-existing --user 0 $pkg
```

Pada ROM lain, bentuk alternatifnya dapat berupa `pm install-existing --user 0`; periksa bantuan ROM. Pada TS7 pengembang, **`cmd package install-existing`** berhasil, sedangkan `pm install-existing` tidak didukung. Jika paket dinonaktifkan sebelum uninstall, pulihkan juga status enabled sesuai catatan awal menggunakan perintah di atas.

Jika muncul `Unknown package`, sistem tidak lagi memiliki paket yang dapat dipulihkan dengan cara itu. Gunakan installer resmi yang cocok atau bantuan vendor. `install-existing` tidak mengunduh APK maupun memulihkan data yang sudah dihapus. Jangan gunakan prosedur ini untuk mengaktifkan kembali APK JarService yang teridentifikasi berbahaya.

### Skala animasi

Gunakan folder `$snapshot` dari sesi yang melakukan perubahan, atau isi ulang dengan lokasi folder yang disimpan:

```powershell
$savedAnimation = Get-Content "$snapshot\animations-before.json" -Raw | ConvertFrom-Json
foreach ($property in $savedAnimation.PSObject.Properties) {
    if ($property.Value -eq 'null') {
        & $adb -s $serial shell settings delete global $property.Name
    } else {
        & $adb -s $serial shell settings put global $property.Name $property.Value
    }
}
```

Nilai semula `null` berarti override tidak disetel, sehingga pemulihan menghapus override, bukan memaksakan angka `1`.

## 9. Catat hasil akhir

```powershell
& $adb -s $serial shell pm list packages --user 0 | Set-Content "$snapshot\installed-after.txt"
& $adb -s $serial shell pm list packages -d --user 0 | Set-Content "$snapshot\disabled-after.txt"
& $adb -s $serial shell dumpsys meminfo | Set-Content "$snapshot\memory-after.txt"
& $adb -s $serial shell dumpsys webviewupdate | Set-Content "$snapshot\webview-after.txt"
& $adb -s $serial get-state
```

Catat paket yang diubah dan hasil pengujian fungsi kendaraan. Bandingkan memori pada kondisi penggunaan serupa; snapshot sesaat tidak membuktikan kenaikan FPS. Contoh hasil pengembang tersedia di [DEBLOAT-TS7.md](DEBLOAT-TS7.md).

Setelah selesai, matikan debugging yang tidak diperlukan. Pada firmware TS7 pengembang, properti Wi-Fi yang sebelumnya diaktifkan dengan `setprop persist.wifi.debug 1` dapat dikembalikan ke `0` dari Terminal head unit; perilakunya khusus firmware. Matikan juga USB/wireless debugging melalui menu perangkat dan pastikan PC tidak lagi dapat tersambung.

Referensi perintah: [ADB dan package manager resmi Android](https://developer.android.com/tools/adb#pm).

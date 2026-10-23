# Kembali ke launcher lama

Installer proyek ini **tidak menghapus atau menonaktifkan launcher OEM**. Simpan launcher lama selama pengujian.

## Melalui pengaturan Android

Jika tersedia pada ROM Anda, buka aplikasi default → aplikasi Home, lalu pilih launcher lama. Menu dan namanya bergantung pada vendor.

## Melalui backup skrip

`Install.ps1` menyimpan komponen HOME sebelumnya di `output/device-backups/` bila dapat dikenali. Lihat file backup untuk perangkat Anda, lalu:

```powershell
.\scripts\Restore-Home.ps1 -Serial '192.168.1.50:5555' -BackupFile '.\output\device-backups\YYYYMMDD-HHMMSS-home.json'
```

Skrip memeriksa bahwa serial backup cocok. DriveLite tetap terpasang; hanya pilihan HOME yang diubah.

## Jika komponen HOME dicatat manual

Sebelum mengubah launcher, catat hasil:

```powershell
$adb = '.\.tools\platform-tools\adb.exe'
& $adb -s '192.168.1.50:5555' shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

Hasil berbentuk `nama.paket/NamaActivity`. Gunakan nilai perangkat Anda, bukan nama paket dari merek lain:

```powershell
.\scripts\Restore-Home.ps1 -Serial '192.168.1.50:5555' -Component 'nama.paket/NamaActivity'
```

Jika launcher OEM pernah dihapus/disabled oleh alat lain, memilih HOME saja tidak memulihkannya. Pulihkan paket tersebut berdasarkan catatan debloat perangkat Anda terlebih dahulu. Proyek ini tidak menjalankan daftar debloat umum karena layanan radio, Bluetooth, kamera mundur, dan tombol setir berbeda antar-ROM.

## Menghapus DriveLite

Setelah launcher lama sudah kembali berfungsi:

```powershell
& $adb -s '192.168.1.50:5555' uninstall id.adhit.drivelite
```

Uninstall menghapus data favorit DriveLite. Untuk update biasa dengan kunci sama, gunakan `install -r`, bukan uninstall.

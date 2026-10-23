# DriveLite Headunit

HU terasa berat? Rapikan aplikasi bawaan dan gunakan launcher ringan dengan favorit, wallpaper, jam, serta kecepatan GPS. **AI mengurus setup PC, build, dan pemasangan.**

![Tampilan DriveLite di head unit](docs/images/home.png)

Screenshot memakai artwork game opsional. AI akan membantu memilih artwork saat membangun launcher.

## Mulai di sini

Siapkan PC Windows, Wi-Fi/hotspot, dan flashdisk. Lakukan langkah di HU saat mobil terparkir.

### 1. Pastikan HU kalian tipe TS7

Buka **Settings → About / About Device** pada HU. Cari tulisan **TS7** pada informasi platform atau versi MCU. Unit contoh proyek ini memakai TS7/SL8141E dengan layar 1024 × 600.

Jika tipenya berbeda, beri tahu AI nanti agar kesesuaiannya diperiksa terlebih dahulu.

### 2. Buka Developer options

Masuk ke pengaturan Android dan buka **Developer options**. Jika belum muncul, cari **Build number**, lalu ketuk tujuh kali.

Jika meminta kode, masukkan **`7890HH`**. `HH` mengikuti **jam yang tampil di HU**, dua digit dalam format 24 jam, tanpa menit:

- HU menunjukkan **18:45** → `789018`.
- HU menunjukkan **07:30** → `789007`.

Setelah masuk, nyalakan tombol **Developer options** jika masih mati.

### 3. Aktifkan USB debugging

Nyalakan **USB debugging**, lalu masukkan password **`john@tw-desktop`**. Ketik huruf kecil, tanpa spasi, lalu tekan **OK**.

<details>
<summary>Lihat contoh dialog pada HU</summary>

![Dialog USB debugging TS7](docs/images/usb-debugging-ts7.jpg)

</details>

Kode di atas khusus firmware TS7 yang dibahas di proyek ini. Jika ditolak, minta kode sesuai firmware kepada penjual.

### 4. Hubungkan HU dan PC ke Wi-Fi yang sama

Bisa menggunakan hotspot HP. Buka detail Wi-Fi pada HU dan catat **alamat IP** yang tampil. AI akan menanyakannya nanti.

### 5. Aktifkan sambungan Wi-Fi untuk debugging

Jika belum ada Terminal di HU, unduh [Terminal Emulator dari pengembangnya](https://jackpal.github.io/Android-Terminal-Emulator/downloads/Term.apk) di PC, salin ke flashdisk, lalu pasang di HU.

Buka aplikasi **Terminal di HU**, ketik perintah berikut, lalu tekan Enter:

```sh
setprop persist.wifi.debug 1
```

Jika muncul error, catat atau foto pesannya untuk ditunjukkan kepada AI. AI akan membantu memeriksa cara koneksi yang cocok.

### 6. Buka AI di PC

Buat folder kosong bernama **DriveLite** di PC. Pilih **salah satu** aplikasi di bawah, login, lalu buka folder tersebut:

| AI yang digunakan | Yang dibuka |
| --- | --- |
| **Claude** | Buka aplikasi Claude Desktop → tab **Code** → pilih **Local** dan folder DriveLite. [Petunjuk resmi](https://code.claude.com/docs/en/desktop) |
| **ChatGPT** | Buka aplikasi ChatGPT desktop → pilih **Codex** → **This computer / Local** → buka folder DriveLite. [Petunjuk resmi](https://learn.chatgpt.com/docs/environments/modes) |
| **Antigravity** | Buka aplikasi Antigravity → **New Project → Add Folder** → pilih folder DriveLite → mulai percakapan dengan mode **Local**. [Petunjuk resmi](https://antigravity.google/docs/getting-started) |

Gunakan mode lokal yang bisa membaca folder dan menjalankan perintah di PC, supaya AI dapat tersambung ke HU.

### 7. Kirim prompt, lalu ikuti percakapan AI

**[Buka file MULAI-DENGAN-AI.txt](MULAI-DENGAN-AI.txt)** atau **[buka versi teks untuk disalin](https://raw.githubusercontent.com/adit7494/drivelite-headunit/main/MULAI-DENGAN-AI.txt)**.

Salin seluruh isi file, tempel ke percakapan AI, lalu kirim. **Langsung pakai; tidak ada bagian yang perlu diisi atau diedit.**

AI akan menanyakan IP HU, memeriksa aplikasi, dan menanyakan fitur yang kalian gunakan. Setelah melihat rekomendasinya, kalian tinggal memilih aplikasi yang ingin dipertahankan atau dinonaktifkan. Setujui dialog **Allow debugging** pada HU jika muncul dari PC kalian.

Selanjutnya AI menyiapkan semua alat, membangun launcher, memasangnya melalui Wi-Fi, dan memandu pengujian. **Kalian tidak perlu memasang SDK, menjalankan build, atau menyalin APK launcher sendiri.** Launcher bawaan disimpan untuk pemulihan.

---

<details>
<summary>Screenshot lainnya</summary>

![Semua aplikasi](docs/images/all-apps.png)

![Menu aplikasi](docs/images/app-menu.png)

</details>

<details>
<summary>Referensi untuk AI, pemulihan, dan pengembang</summary>

- [Panduan penggunaan](docs/USAGE.md) · [Pemulihan](docs/RECOVERY.md) · [Troubleshooting](docs/TROUBLESHOOTING.md).
- [Perintah debloat](docs/DEBLOAT-MANUAL.md) · [Catatan hasil TS7](docs/DEBLOAT-TS7.md).
- [Setup/pemasangan teknis](docs/INSTALL.md) · [Build/artwork](docs/BUILD.md) · [Instruksi AI](AGENTS.md).
- [APK demo](https://github.com/adit7494/drivelite-headunit/releases/latest) · [GitHub Actions](https://github.com/adit7494/drivelite-headunit/actions) · [Kontribusi](CONTRIBUTING.md) · [Mengelola rilis](docs/MAINTAINING.md).
- Perangkat yang diuji: TS7, Android 8.1/API 27, ARMv7, 1024 × 600. Angka versi Android pada menu HU dapat berbeda; AI memeriksa versi sebenarnya melalui ADB.
- Kode menu dan metode Wi-Fi TS7: [laporan pemilik perangkat](https://4pda.to/forum/index.php?showtopic=1028320&st=2480), [contoh koneksi](https://4pda.to/forum/index.php?showtopic=1028320&st=500).

</details>

Kode dan dokumentasi berlisensi [MIT](LICENSE). Artwork game milik penerbit masing-masing; lihat [NOTICE.md](NOTICE.md). DriveLite merupakan proyek independen, tidak berafiliasi dengan Sony/PlayStation.

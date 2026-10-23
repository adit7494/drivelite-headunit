# Menggunakan launcher

## Beranda

![Beranda pada TS7 1024 × 600](images/home.png)

1. **Ikon kotak kiri atas:** berpindah antara favorit dan seluruh aplikasi. Tidak ada tab teks.
2. **Kartu di atas:** hanya aplikasi favorit. Sentuh untuk memilih; cover membesar dan wallpaper mengikuti pilihan.
3. **Panel kecil kiri bawah:** cover pilihan, nama aplikasi, tombol Buka, serta menu `•••`.
4. **Navigasi tengah bawah:** kiri/kanan memilih favorit; Enter membuka aplikasi terpilih.
5. **Kanan atas:** pencarian, pengaturan, dan jam 24 jam.
6. **Kanan bawah:** kecepatan GPS, ikon gerakan, dan indikator koneksi.

Screenshot menggunakan artwork game opsional, bukan artwork demo pada APK publik.

## Tambah dan hapus favorit

![Semua aplikasi](images/all-apps.png)

Buka ikon kotak, lalu tekan tahan aplikasi yang diinginkan.

![Menu tekan tahan](images/app-menu.png)

- **Tambah favorit:** tampilkan pada beranda.
- **Hapus dari favorit:** hilangkan dari beranda, aplikasinya tetap terpasang.
- **Hapus aplikasi:** buka konfirmasi uninstaller Android untuk aplikasi pengguna. Aplikasi sistem/vendor dilindungi dari penghapusan melalui launcher.
- **Info aplikasi:** buka detail aplikasi Android.

Fav­orit disimpan lokal dan tetap ada setelah launcher dibuka ulang. Hingga 25 aplikasi mendapat cover berbeda. Lebih dari 25 aplikasi tetap dapat ditampilkan, tetapi sebagian cover akan digunakan ulang.

## Arti status GPS

| Tampilan | Arti |
| --- | --- |
| Bulat hijau, GPS TERHUBUNG | Data GPS valid dan masih baru |
| Bulat merah, MENCARI GPS / GPS NONAKTIF | Belum ada data valid atau GPS dimatikan |
| `—` | Kecepatan belum tersedia; tidak diganti dengan angka nol palsu |
| 0 km/h, Mobil berhenti | Data 0–2 km/h; 1–2 ditekan menjadi 0 untuk mengurangi drift |
| 3–4 km/h, Bergerak pelan | Data bergerak rendah setelah melewati filter |
| Mulai 5 km/h, Mobil berjalan | Data bergerak dari GPS |

Kecepatan berasal dari GPS Android, bukan CAN/ECU atau status mesin. Data lebih dari 10 detik, mock location, data tanpa speed, atau kualitas di bawah batas kode tidak dipakai. GPS memerlukan izin lokasi dan sinyal yang memadai; ketuk panel GPS untuk pengaturan lokasi/izin.

## Wallpaper dan performa

Roda gigi menyediakan pergantian wallpaper berikutnya dan sakelar otomatis 25 detik. Pergantian otomatis hanya berjalan di beranda saat activity aktif. GPS berhenti ketika berada di daftar aplikasi atau aplikasi lain. Tidak ada service latar, iklan, WebView, atau izin internet pada launcher.

Gambar diproses di worker tunggal; hanya gambar aktif dan incoming yang dipertahankan selama crossfade. Perangkat TS7 lama dapat melewatkan frame; proyek tidak menjanjikan 60 fps stabil.

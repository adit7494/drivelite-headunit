# Kontribusi

1. Buat issue yang menjelaskan masalah atau perubahan yang diinginkan.
2. Untuk bug, sertakan versi Android dari `getprop`, resolusi, versi DriveLite, langkah reproduksi, dan screenshot yang sudah dibersihkan dari data pribadi.
3. Buat branch perubahan dari `main`.
4. Jalankan `build.ps1 -Artwork Demo` dan `test.ps1` di Windows.
5. Jika mengubah UI, periksa screenshot beranda dan semua aplikasi. Jika mengubah GPS, periksa keadaan tanpa fix serta filter drift.
6. Kirim PR berisi masalah, perilaku sesudah perubahan, dan pemeriksaan yang dilakukan.

Pertahankan launcher native dan hemat sumber daya. Hindari service latar, ketergantungan besar, internet runtime, atau penghapusan paket vendor secara otomatis tanpa kebutuhan yang jelas. Tes logika tidak menggantikan pengujian perangkat.

Jangan commit keystore, token, diagnosis lokasi, atau artwork pihak ketiga. Source URL/checksum dapat ditambahkan ke katalog, sedangkan file asli diunduh lokal.

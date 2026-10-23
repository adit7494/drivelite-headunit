# Mengelola repository dan rilis

Repository: <https://github.com/adit7494/drivelite-headunit>.

## Perubahan biasa

```powershell
git status
git switch -c nama-perubahan
# Edit kode atau panduan.
.\build.ps1 -Artwork Demo
.\test.ps1
git add README.md docs src scripts tests
git diff --cached
git commit -m "Describe the change"
git push -u origin nama-perubahan
```

Buat pull request, periksa hasil **Build and test**, lalu merge setelah pemeriksaan selesai. Hindari `git add -f` pada signing, artwork unduhan, log, atau backup perangkat.

## Membuat release

1. Perbarui versionCode/versionName di `AndroidManifest.xml` dan [CHANGELOG.md](../CHANGELOG.md).
2. Build `-Artwork Demo` dengan keystore rilis yang sama seperti sebelumnya.
3. Jalankan tes dan uji APK di head unit sebelum mengubah HOME default.
4. Pastikan `output/SHA256SUMS.txt` cocok dengan APK.
5. Commit perubahan dan push branch utama/tag, lalu tunggu CI.
6. Unggah APK demo dan checksum ke Releases. Jangan unggah APK artwork game pribadi.

Contoh dengan GitHub CLI setelah catatan rilis disimpan dalam file:

```powershell
git tag v2.2.1
git push origin v2.2.1
gh release create v2.2.1 --verify-tag --title "DriveLite 2.2.1" --notes-file release-notes.md .\output\DriveLite.apk .\output\SHA256SUMS.txt
```

Nomor tag hanya contoh. Sesuaikan versi Android dan catatan rilis dengan perubahan sebenarnya.

## Menjaga kunci dan data lokal

- Simpan backup keystore di tempat privat; repository publik tidak cukup untuk menghasilkan update bertanda tangan sama tanpa kunci itu.
- `signing/`, `.tools/`, `build/`, `output/`, `runs/`, dan gambar game unduhan diabaikan Git.
- Jangan unggah diagnosis GPS, dump perangkat, token GitHub, atau file `.env`.
- APK CI memakai kunci runner yang dibuat saat itu. Gunakan keystore pemelihara yang dipertahankan untuk rilis publik berikutnya.

# 🚀 Lite Boost Android (Simple Optimizer)

Sebuah aplikasi utilitas Android super ringan, bergaya modern (Material 3), yang dibuat khusus untuk membersihkan memori internal dan mengosongkan RAM tanpa memerlukan akses Root.

## ✨ Fitur Utama
1. **Dasbor Pro (Real-Time System Monitor)**: Memantau sisa *Storage* (Memori Internal) dan RAM HP Anda secara langsung di halaman utama.
2. **Junk Cleaner (Transparansi Data 2-Tahap)**: 
   - **Tahap 1 (Scan):** Memindai seluruh folder penyimpanan untuk mendata file sampah (ekstensi `.tmp`, `.log`, `.bak`, `.exo`, dan folder `cache`) tanpa menghapusnya terlebih dahulu. File penting Anda 100% aman.
   - **Tahap 2 (Review & Hapus):** Menampilkan *log* daftar ratusan file kotor secara transparan, memberikan Anda kendali penuh untuk menekan tombol **"Hapus Sekarang"** hanya jika Anda merasa yakin.
3. **Fast Reboot & Speed Up**: Menggunakan fungsi bawaan sistem operasi Android untuk mematikan secara paksa (*Force Stop*) aplikasi-aplikasi pihak ketiga yang menyedot RAM di latar belakang. Membuat HP kembali responsif layaknya baru direstart!

## 📱 Kompatibilitas
- ✅ **Android 9 (Pie)** - Bekerja sempurna dengan akses penuh.
- ✅ **Android 10 (Q)** - Bekerja sempurna (memanfaatkan *Legacy External Storage*).
- ✅ **Android 11+ (R)** - Bekerja secara aman (aplikasi memiliki sistem pertahanan internal yang secara otomatis akan meminta izin *All Files Access* di menu Pengaturan Privasi sebelum mengizinkan proses pemindaian).

## 🛠️ Cara Mengunduh (Download APK)
Aplikasi ini di-build secara otomatis menggunakan sistem **Cloud Build (GitHub Actions)**. Anda tidak perlu repot melakukan kompilasi di PC Anda!
1. Pergi ke tab menu **Actions** di atas.
2. Klik proses *build* terbaru yang berlogo **Ceklis Hijau**.
3. Gulir ke bawah halaman hingga menemukan bagian **Artifacts**.
4. Klik tulisan **`Optimizer-App-APK`** untuk mendownload file `.zip`.
5. Ekstrak file zip tersebut, kirimkan `app-debug.apk` ke HP Android Anda, dan klik untuk mulai **Instal**!

---
*Dibuat menggunakan keajaiban Jetpack Compose & Kotlin.*

# 🚀 Lite Boost Android (Pro Version)

Sebuah aplikasi utilitas Android super ringan, bergaya **Gen-Z Neo-Brutalism**, yang dibuat khusus untuk membersihkan memori internal dan mengosongkan RAM tanpa memerlukan akses Root.

## ✨ Fitur Utama
- [x] **Gen-Z Minimalist UI** (Monochrome Neo-Brutalism, No Gradients)
- [x] **Battery & Thermal Monitoring** (Real-time tracking of battery temperature and health)
- [x] **Auto-Pilot Cleaner** (WorkManager integration for background cleaning every 3 days)
- [x] **Work Profile Engine (Dual Space)** (Creates a managed isolated space to clone apps)
- [x] **Anti-Delete Message Vault** (Intercepts and saves all notifications, including deleted WhatsApp messages)
- [x] **DNS-Level Web Shield** (Uses a local VPN to route DNS queries to AdGuard, blocking ads system-wide)
- [x] **iClone Pro Camera (Cinematic Engine)** (Uses CameraX API & OEM Extensions to force Hardware HDR and Zero Shutter Lag, mimicking iPhone camera clarity)
- [x] **Pro Studio AI (Editor Ultimate Edition)** (Hardware-accelerated 1080p 60fps video editor using Media3 Transformer, featuring mock AI Magic Eraser, Video Matting, and Optical Flow modules)
- [x] **Junk Cleaner (Transparansi Data 2-Tahap)**: 
    - **Tahap 1 (Scan):** Memindai seluruh folder penyimpanan untuk mendata file sampah tanpa menghapusnya terlebih dahulu. File penting Anda 100% aman.
    - **Tahap 2 (Review & Hapus):** Menampilkan *log* daftar file kotor secara transparan, memberikan Anda kendali penuh.

## 🛠️ Persyaratan Sistem Khusus
- Untuk menjalankan **DNS-Level Web Shield**, Anda harus menyetujui prompt "VpnService" yang muncul di layar.
- Untuk menjalankan **Anti-Delete Message Vault**, Anda akan diarahkan ke layar Akses Notifikasi agar aplikasi bisa membaca pesan yang masuk.
- Untuk menjalankan **Work Profile Engine**, Android akan meminta persetujuan untuk membuat Profil Kerja (Kloning Sistem).

## 🔨 Panduan Kompilasi & Instalasi
Aplikasi ini sudah diprogram agar bisa di-*build* sepenuhnya menggunakan **GitHub Actions**.

1. Cukup buka halaman GitHub *Repository* Anda di PC atau HP.
2. Masuk ke *tab* **Actions**.
3. Di sisi kiri, klik **Build Android APK**.
4. Klik tombol **Run workflow**.
5. Tunggu sekitar 2 menit. File `app-release-unsigned.apk` akan muncul di bagian terbawah (*Artifacts*).
6. Unduh file tersebut ke HP Anda dan langsung *Install*! (Abaikan peringatan Play Protect jika ada, karena aplikasi kita adalah aplikasi buatan sendiri).

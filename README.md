# 🚀 Lite Boost Android (Pro Version)

Sebuah aplikasi utilitas Android super ringan, bergaya **Gen-Z Neo-Brutalism**, yang dibuat khusus untuk membersihkan memori internal dan mengosongkan RAM tanpa memerlukan akses Root.

## ✨ Fitur Utama
- [x] **Gen-Z Minimalist UI** (Monochrome Neo-Brutalism, No Gradients)
- [x] **Battery & Thermal Monitoring** (Real-time tracking of battery temperature and health)
- [x] **Auto-Pilot Cleaner** (WorkManager integration for background cleaning every 3 days)
- [x] **Local VPN Firewall** (Block internet access to save data and remove ads)
- [x] **True Hibernation** (Uses AccessibilityService to auto-click Force Stop)
- [x] **Notification Blackhole** (Intercepts and cancels annoying notifications silently)
- [x] **Junk Cleaner (Transparansi Data 2-Tahap)**: 
    - **Tahap 1 (Scan):** Memindai seluruh folder penyimpanan untuk mendata file sampah tanpa menghapusnya terlebih dahulu. File penting Anda 100% aman.
    - **Tahap 2 (Review & Hapus):** Menampilkan *log* daftar file kotor secara transparan, memberikan Anda kendali penuh.

## 🛠️ Persyaratan Sistem Khusus
- Untuk menjalankan **Local Firewall**, Anda harus menyetujui prompt "VpnService" yang muncul di layar.
- Untuk menjalankan **True Hibernation**, Anda akan diarahkan ke Pengaturan -> Aksesibilitas (Accessibility) untuk mengizinkan aplikasi ini mengontrol layar Anda dan memencet tombol "Paksa Berhenti" sendiri.
- Untuk menjalankan **Notification Blackhole**, Anda akan diarahkan ke layar Akses Notifikasi.

## 🔨 Panduan Kompilasi & Instalasi
Aplikasi ini sudah diprogram agar bisa di-*build* sepenuhnya menggunakan **GitHub Actions**.

1. Cukup buka halaman GitHub *Repository* Anda di PC atau HP.
2. Masuk ke *tab* **Actions**.
3. Di sisi kiri, klik **Build Android APK**.
4. Klik tombol **Run workflow**.
5. Tunggu sekitar 2 menit. File `app-release-unsigned.apk` akan muncul di bagian terbawah (*Artifacts*).
6. Unduh file tersebut ke HP Anda dan langsung *Install*! (Abaikan peringatan Play Protect jika ada, karena aplikasi kita adalah aplikasi buatan sendiri).

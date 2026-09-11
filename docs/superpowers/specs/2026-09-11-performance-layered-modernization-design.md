# Performance Layered Modernization — Design

Tanggal: 2026-09-11
Status: Disetujui (brainstorming selesai; eksekusi subagent-driven)
Konteks: OMNIX OS setelah DNS Shield engine. Target: tidak lag, lancar di HP RAM kecil (2–3 GB).

## 1. Tujuan

Menghilangkan lag di semua aspek (startup, scroll dashboard, browsing dengan shield, buka Shield, cleaner) dengan tiga tuas utama yang didukung docs resmi: **upgrade compiler/build stack**, **R8 release build**, dan **Baseline Profile** — ditambah perbaikan hotspot kode yang sudah teridentifikasi dari inspeksi kode nyata.

## 2. Keputusan bersama user

- Scope: **semua aspek** lag.
- **Boleh upgrade dependency** (Kotlin 2.x / Compose modern / AGP 8.5).
- Pengukuran: **Baseline Profile + Macrobenchmark + Gradle Managed Device di CI**.
- Distribusi: **Release + R8, signing debug-key** (bukan Play Store).
- Pendekatan: **A — Layered Modernization** (bertahap, tiap lapis di-gate CI).

## 3. Riset Context7 (dasar desain)

- `developer.android.com` Compose performance: gunakan `remember` untuk kalkulasi mahal, stable keys di lazy layout, `derivedStateOf` untuk state cepat berubah, defer state reads, hindari kerja berat di composition.
- `android/performance-samples`: `BaselineProfileRule.collect(maxIterations/stableIterations)`, `MacrobenchmarkRule.measureRepeated` dengan `StartupTimingMetric`/`FrameTimingMetric`, `CompilationMode.Partial(BaselineProfileMode.Require)`, `JankStats` opsional.
- Compose compiler setup Kotlin 2.0: plugin `org.jetbrains.kotlin.plugin.compose` (versi = versi Kotlin), hapus `composeOptions.kotlinCompilerExtensionVersion`, opsi via blok `composeCompiler {}`.

## 4. T2 — Target versi upgrade

| Komponen | Sekarang | Target | Catatan |
|---|---|---|---|
| Kotlin | 1.8.10 | 2.0.21 | K2; fallback 1.9.24 bila kapt bermasalah |
| Compose BOM | 2023.08.00 | 2024.09.03 | strong skipping; fallback 2024.06.00 |
| AGP | 8.1.0 | 8.5.2 | prasyarat plugin baseline-profile & GMD |
| Compose compiler | ext 1.4.3 | plugin `org.jetbrains.kotlin.plugin.compose` = versi Kotlin | cara resmi baru |
| Hilt | 2.48 | 2.52 | kapt dipertahankan; KSP hanya jika perlu |
| WorkManager / hilt-work | 2.8.1 / 1.0.0 | 2.9.1 / 1.2.0 | |
| CameraX / Media3 | 1.3.1 / 1.3.0 | 1.4.0 / 1.4.1 | |
| core-ktx / lifecycle / activity-compose | 1.10.1 / 2.6.1 / 1.7.2 | 1.13.1 / 2.8.4 / 1.9.2 | |
| profileinstaller | — | 1.3.1 | syarat baseline profile |

Versi final divalidasi saat eksekusi via CI; setiap penyimpangan dicatat.

## 5. T3 — Rilis R8

- `release`: `isMinifyEnabled = true`, `isShrinkResources = true`, `signingConfig = signingConfigs.getByName("debug")`.
- `proguard-rules.pro`: keep rules Hilt/WorkManager/Media3/CameraX + komponen manifest (VpnService, NotificationListenerService, AccessibilityService, DeviceAdminReceiver).
- CI artifact berubah: `app-release.apk` (signed debug-key).
- QA device wajib setelah T3 (R8 bisa mem-break fitur tanpa gagal compile).

## 6. T4 — Hotspot fixes (teridentifikasi dari kode)

### A. Shield repository (ANR risk)
- `init` tidak parse di main thread; parse di `Dispatchers.Default`, hasil di-publish ke sets ConcurrentHashMap + `engineVersion`.
- `reloadLists()` re-parse **hanya list yang berubah** (parameter opsional list-id), bukan 3 list sekaligus.
- Allowlist diekspos sebagai `StateFlow<Set<String>>` — UI tidak lagi pakai `allowTick` hack.

### B. DNS engine async
- Engine loop: read/parse/decide tetap sinkron (block <1ms).
- Jalur Allow di-dispatch per-query ke `ioScope` dengan `Semaphore(16)` batas in-flight.
- Write balik ke TUN lewat single-writer (`Mutex` atau channel), karena `FileOutputStream` tidak thread-safe.
- Forward timeout tetap 5s per query, tapi tidak lagi memblokir query lain.

### C. Compose hotspots
- `StatusCard`: pulse `infiniteTransition` hanya aktif saat `alert` (subkomposisi terpisah), bukan selalu ×4 kartu.
- `TerminalLog`: `.map()`/`takeLast` dibungkus `remember(logs)`; migrasi `animateItemPlacement` → `animateItem` (BOM baru).
- `ShieldActivity`: `Column` + `verticalScroll`; allowlist dari StateFlow.
- Verifikasi ulang stagger 12 kartu setelah upgrade BOM.

### D. Startup
- `FilterListUpdateWorker.schedule()` dipindah dari `OmnixApplication.onCreate` ke `MainActivity.onCreate` (hindari init disk I/O WorkManager di startup path).

### E. Small behavioral
- `System.gc()` boost RAM dipindah ke IO + ditunda 200ms agar tidak menghambat frame tombol.

## 7. T5 — Baseline Profile & Benchmark di CI

- Module `:baselineprofile` (plugin `androidx.baselineprofile` + `com.android.test`), GMD AOSP ATD image.
- `BaselineProfileGenerator`: journey cold start → dashboard scroll → buka Shield → scroll → back. `maxIterations=15`, `stableIterations=3`.
- `StartupBenchmark`: `StartupTimingMetric()` + `CompilationMode.Partial(BaselineProfileMode.Require)`.
- `FrameTimingBenchmark`: `FrameTimingMetric()` scroll dashboard + Shield.
- Output profil ditulis ke `app/src/main/baselineProfiles/` dan di-commit.
- CI: job benchmark **manual/nightly** (bukan tiap push) untuk hemat kuota; job build tetap cepat.
- T1 (capture "before") dan T5 (after) dijalankan dengan prosedur sama supaya perbandingan adil.

## 8. Target metrik

| Metrik | Sebelum (diukur T1) | Target |
|---|---|---|
| Cold start P50 (partial+profile) | diukur | ≤500ms |
| Frame P90 scroll dashboard | diukur | <16ms |
| Buka ShieldActivity render | ~1–2s jank (parse main) | <300ms render; rules siap <2s tanpa jank |
| APK size release | diukur (debug) | ≥30% lebih kecil dari debug |
| Blocked query latency | ≤1ms | tetap ≤1ms |

Gap yang tidak tercapai wajib didokumentasikan, bukan disembunyikan.

## 9. Tahapan & gate

T1 baseline capture → T2 upgrade (CI hijau) → T3 R8 release (CI hijau + QA) → T4 hotspot (CI hijau + QA per kelompok) → T5 baseline profile + benchmark after → T6 verifikasi final & merge.
Regresi vs T1 pada tahap mana pun = stop dan evaluasi.

## 10. Risiko & fallback

- **K2 vs kapt/CameraX gagal** → fallback Kotlin 1.9.24 + Compose BOM 2024.06 (tanpa strong skipping); keputusan saat eksekusi, dicatat.
- **GMD gagal di runner** → benchmark di-trigger manual; bila tetap gagal, generate profil via instruksi lokal + benchmark dilewati dengan catatan.
- **R8 mem-break fitur** → keep rules + QA checklist per fitur; CI tidak mendeteksi runtime, QA wajib.
- **Sisa lag di layar berat (ProStudio/Camera)** → spec lanjutan (measure-first), bukan diperluas diam-diam.

## 11. YAGNI

JankStats produksi, Perfetto deep-dive, refactor ProStudio/Camera, Play Store signing, multi-module restructure selain `:baselineprofile`/`:app`.

## 12. Kriteria selesai

- [ ] Stack ter-upgrade; `assembleDebug` + `assembleRelease` hijau.
- [ ] APK release R8 ≥30% lebih kecil dari debug & install/update normal.
- [ ] Hotspot A–E selesai; tidak ada parse >100ms di main thread saat buka Shield.
- [ ] Baseline profile ter-commit & terpakai; startup Partial+Require lebih baik dari None.
- [ ] Benchmark after memenuhi/mendekati target §8 (gap terdokumentasi).
- [ ] QA device RAM kecil lulus checklist.

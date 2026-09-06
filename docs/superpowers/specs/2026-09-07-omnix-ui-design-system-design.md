# OMNIX OS — UI/UX Design System Revamp ("B Disiplin")

Tanggal: 2026-09-07
Status: Disetujui (brainstorming selesai, menunggu implementasi plan)

## 1. Tujuan & Latar

UI OMNIX OS saat ini fungsional tapi terasa "AI slop": semua kartu identik, tanpa animasi, tanpa hierarki, warna datar hitam-putih pekat. Tujuan revamp: membuat aplikasi terasa **hidup dan punya karakter** saat dibuka — tanpa meninggalkan identitas Neo-Brutalism.

**Keputusan bersama user:**
- Identitas: **Evolusi** — brutalism + aksen warna (bukan monokrom murni, bukan ganti gaya).
- Cakupan: **Design system dulu** (token + komponen + micro-animasi). Layar lain mengikuti otomatis.
- Aksen warna: **Semantic multi-accent per fitur** (satu warna per fitur).
- Pendekatan: **B disiplin** — fondasi token + efek fisik terpilih (haptics, magnetic press, glow aktif, spring physics), tanpa gadget-show.

## 2. Arsitektur

Layer baru `com.optimizer.android.ui.theme`:

```
ui/theme/
├── OmnixColors.kt      // token warna semantik
├── OmnixTypography.kt  // type scale
├── OmnixMotion.kt      // durasi, easing, spring specs
├── OmnixHaptics.kt     // pola haptic terpola
└── OmnixTheme.kt       // CompositionLocal OmnixColors + MaterialTheme wrapper
```

**Prinsip:** komponen tidak boleh hardcode warna. Semua akses lewat `OmnixTheme.colors`. Font assets di `res/font/`.

Komponen UI di-refactor, **ViewModel / Service / Repository tidak disentuh**.

## 3. Color Tokens

| Token | Hex | Fungsi |
|---|---|---|
| `Base` | `#0A0A0A` | background (hitam diangkat, bukan 000000 pekat) |
| `Ink` | `#F2F2F2` | teks utama off-white |
| `Grid` | `#7A7A7A` | teks sekunder, label mono |
| `Cleaner` | `#C6FF00` | Volt Lime — Junk/RAM cleaner |
| `Shield` | `#00E5FF` | Cyan — DNS Web Shield |
| `Vault` | `#B388FF` | Violet — Anti-Delete Vault |
| `Camera` | `#FF4D9D` | Magenta — iClone Camera |
| `Studio` | `#FF8A00` | Orange — Pro Studio |
| `Danger` | `#FF3B30` | alert termal/baterai |

**Aturan penggunaan:** satu kartu fitur = satu aksen. Aksen hanya di: rail border kiri kartu, ikon, tombol konfirmasi, state aktif. Sisanya hitam-putih.

## 4. Tipografi

Dua font baru di `res/font/`:
- **Space Grotesk** (Bold/Black) — judul, angka status.
- **JetBrains Mono** (Regular/Medium) — label kecil, log, data teknis.

Type scale: Display 34sp/Black (OMNIX OS) → Title 20sp/Bold → Body 14sp → Label 11sp Mono uppercase letterspacing 1.5sp.

## 5. Motion & Interaksi

### Motion tokens (OmnixMotion.kt)

| Token | Nilai | Dipakai untuk |
|---|---|---|
| `Instant` | 90ms | state toggle kecil |
| `Quick` | 150ms | press feedback, border color |
| `Standard` | 250ms | masuk/keluar konten |
| `Emphasis` | 350ms | dialog, transisi penting |
| `Spring.snap` | `spring(dampingRatio = 0.6f, stiffness = 500f)` | magnetic press |
| `Easing.ambient` | `FastOutSlowInEasing` | fade/slide ambient |

**Aturan anti-slop:** semua animasi 90–350ms. Tidak ada loop dekoratif kecuali pulse alert (2s, halus).

### Interaksi

1. **Magnetic press** — kartu scale 1f→0.97 + shift ~2dp saat ditekan; lepas → snap balik spring bouncy + haptic tick.
2. **Staggered entrance** — header dulu, lalu kartu fade+slide-up jeda 40ms antar item.
3. **Count-up angka** — STORAGE/RAM/TEMP via `animateIntAsState` spring.
4. **Accent border transition** — warna border/rail via `updateTransition`.
5. **Alert pulse** — border merah berdenyut (alpha 1.0→0.55, `infiniteTransition`) saat temp > 40°C; haptic buzz sekali saat alert pertama.
6. **Dialog pop-in** — scrim fade 150ms, konten scale 0.9→1.0 spring overshoot; tombol GASKAN pakai aksen fitur.
7. **Log terminal hidup** — baris baru fade+slide via `LazyColumn` + `animateItem` (dengan key); limit 50 baris.
8. **animateContentSize** — kartu yang expand/collapse teranimasi mulus.

### Haptics

- Tap kartu → tick ringan (sekali)
- Konfirmasi sukses → double-tick
- Alert muncul → satu buzz panjang ringan

## 6. Komponen

| Komponen | Perubahan |
|---|---|
| `StatusCard` | count-up, border accent via updateTransition, alert pulse, mono label |
| `SuperpowerCard` | accent rail kiri 4dp, ikon kotak outline, magnetic press, haptic, glow halus via `shadowElevation` tint aksen saat pressed (bukan blur shader) |
| `NeoDialog` | pop-in spring, scrim fade, tombol konfirmasi aksen fitur |
| `SectionHeader` (baru) | judul section + nomor mono (`01 /`) untuk hierarki |
| `TerminalLog` (baru) | konsol: animateItem, limit 50 baris, blinking cursor, empty state |

## 7. States

- **Empty** — `> AWAITING SIGNAL...` mono redup + blinking cursor (bukan layar kosong).
- **Progress** — cleaner scan: progress bar blok brutal (`████░░░░`) mono + aksen fitur (butuh state `scanProgress: Float` kecil di MainViewModel).
- **Alert** — token Danger + pulse + haptic, terpusat di StatusCard.

## 8. YAGNI — Sengaja Tidak Dikerjakan

Splash custom, shimmer, marquee ticker, shader glow, onboarding, light theme, animasi layar kamera/studio (menyusul), refactor ViewModel/Service.

## 9. Verifikasi

1. `@Preview` per komponen (normal / alert / pressed).
2. Gate CI: workflow `android-build.yml` tetap hijau.
3. QA manual di device: stagger masuk, magnetic press, haptics tidak berlebihan, alert pulse hanya saat alert.

## 10. Kriteria Selesai

- [ ] Layer `ui/theme/` lengkap (colors, typography, motion, haptics, theme).
- [ ] Font Space Grotesk + JetBrains Mono ter-bundle di `res/font/`.
- [ ] Semua komponen di `OmnixComponents.kt` mengkonsumsi token, tanpa hardcode warna.
- [ ] `MainActivity` render pakai komponen baru + staggered entrance.
- [ ] Build GitHub Actions sukses.

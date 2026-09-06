# OMNIX UI Design System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mengimplementasikan design system "B Disiplin" dari spec `docs/superpowers/specs/2026-09-07-omnix-ui-design-system-design.md` — layer theme token (colors, typography, motion, haptics), rewrite komponen (StatusCard, SuperpowerCard, NeoDialog, SectionHeader, TerminalLog), dan integrasi ke MainActivity.

**Architecture:** Clean token layer di `ui/theme/` dengan `CompositionLocal`, komponen di `ui/components/` mengkonsumsi token lewat `OmnixThemeColors.colors`. ViewModel hanya dapat 1 tambahan field (`scanProgress`). Tidak ada perubahan dependency/gradle.

**Tech Stack:** Kotlin 1.8.10, Jetpack Compose (BOM 2023.08.00 → ui/foundation/animation 1.5.0, material3 1.1.x), Material3, WorkManager (tidak disentuh).

## Global Constraints

- **TIDAK ADA build lokal** — komputer ini tidak punya gradlew/Android SDK. Verifikasi kompilasi = **GitHub Actions** (workflow `android-build.yml`, `gradle assembleDebug`). Commit harus selalu *individually buildable*.
- Workflow CI trigger otomatis hanya di push ke `main`/`master`; untuk verifikasi branch gunakan `workflow_dispatch` pada ref branch tersebut.
- Compose BOM 2023.08.00 = **Compose 1.5.0**: gunakan `Modifier.animateItemPlacement()` (BUKAN `animateItem()` — itu Compose 1.7+). `togetherWith` tersedia di animation 1.5.0. `HapticFeedbackType` hanya punya `TextHandleMove` (tick ringan) dan `LongPress` (buzz).
- Material3 1.1 tidak bisa men-tint elevation shadow → "glow" diimplementasikan sebagai **hard accent shadow** (Box aksen offset 4dp di belakang kartu, alpha dianimasikan) — lebih on-brand neo-brutalism.
- minSdk 28, compileSdk 34, Java 17. Jangan ubah `build.gradle.kts`, `settings.gradle.kts`, atau dependency apa pun.
- Semua warna di komponen WAJIB lewat token (`OmnixThemeColors.colors`); 0 hardcode warna di komponen.
- Animasi 90–350ms. Satu-satunya loop dekoratif = pulse alert (2000ms) dan blink cursor (500ms).
- Palet & aturan aksen: verbatim dari spec §3 — satu kartu fitur = satu aksen; aksen hanya di rail/ikon/tombol konfirmasi/state aktif.
- Mapping fitur→aksen (spec §3 + keputusan desain):
  - `JUNK & CACHE CLEANER`, `RAM SPEED BOOSTER`, `DEEP ROOT APP ERASER` → `CLEANER` (lime `#C6FF00`)
  - `DNS-LEVEL WEB SHIELD` (dialog VPN) → `SHIELD` (cyan `#00E5FF`)
  - `ANTI-DELETE MESSAGE VAULT` (dialog BLACKHOLE) → `VAULT` (violet `#B388FF`)
  - `WORK PROFILE ENGINE` (dialog HIBERNATION) → ink `#F2F2F2` (netral sistem)
  - `iCLONE PRO CAMERA` → `CAMERA` (magenta `#FF4D9D`)
  - `PRO STUDIO AI` → `STUDIO` (orange `#FF8A00`)
  - Kartu status (STORAGE/RAM/TEMP/HEALTH) → ink; alert → `danger` (`#FF3B30`)
- Copy Indonesia yang ada (`GASKAN`, `BATAL`, dll.) dipertahankan.
- **Signatur komponen lama dipertahankan kompatibel-mundur**: parameter baru diletakkan SETELAH parameter lama dengan nilai default, supaya setiap commit tetap buildable meski MainActivity belum di-update.
- Spec deviation yang disepakati plan: Space Grotesk tidak punya weight Black (300–700) → Display memakai Bold (700). "Glow" = hard accent shadow (lihat atas).
- Deviasi spesifik §5 (efek setara, lebih sederhana & tahan-banting di Compose 1.5):
  - §5.3 "count-up" angka status → diimplementasi sebagai `AnimatedContent` slide-up+fade **per perubahan nilai** (rasa count-up tanpa interpolasi angka pecahan).
  - §5.8 `animateContentSize` → ditunda: belum ada konten yang expand/collapse di v1; akan dipakai saat layar lain direvamp.
  - §5.4 `updateTransition` untuk border → border alert memakai pulse `infiniteTransition`; `updateTransition` dipakai nanti jika ada toggle aksen runtime.

## File Structure

```
app/src/main/res/font/                      (Task 1) — 3 file .ttf + res/raw/font_licenses.txt
app/src/main/java/com/optimizer/android/ui/theme/
  OmnixColors.kt                            (Task 2) — FeatureType, OmnixColorScheme, LocalOmnixColors
  OmnixMotion.kt                            (Task 3) — durasi/spring/easing token
  OmnixHaptics.kt                           (Task 3) — helper haptic terpola
  OmnixTypography.kt                        (Task 4) — font family + type scale
  OmnixTheme.kt                             (Task 4) — OmnixTheme composable + akses colors
app/src/main/java/com/optimizer/android/ui/components/
  OmnixComponents.kt                        (Task 5,6,7) — StatusCard, SuperpowerCard, NeoDialog, SectionHeader, StaggerIn, BrutalBar
  TerminalLog.kt                            (Task 8) — TerminalLog konsol
app/src/main/java/com/optimizer/android/presentation/
  MainViewModel.kt                          (Task 9) — + scanProgress
app/src/main/java/com/optimizer/android/
  MainActivity.kt                           (Task 10) — integrasi penuh
```

---

### Task 1: Font assets + branch kerja

**Files:**
- Create: `app/src/main/res/font/space_grotesk_bold.ttf`
- Create: `app/src/main/res/font/jetbrains_mono_regular.ttf`
- Create: `app/src/main/res/font/jetbrains_mono_medium.ttf`
- Create: `app/src/main/res/raw/font_licenses.txt`

**Interfaces:**
- Consumes: tidak ada.
- Produces: resource `R.font.space_grotesk_bold`, `R.font.jetbrains_mono_regular`, `R.font.jetbrains_mono_medium` (dipakai Task 4).

- [ ] **Step 1: Buat branch kerja**

```powershell
git checkout -b feat/omnix-design-system
```

- [ ] **Step 2: Download font TTF**

Nama resource harus lowercase-underscore. Jalankan:

```powershell
New-Item -ItemType Directory -Force -Path "app\src\main\res\font" | Out-Null
Invoke-WebRequest -Uri "https://github.com/floriankarsten/space-grotesk/raw/master/fonts/ttf/SpaceGrotesk-Bold.ttf" -OutFile "app\src\main\res\font\space_grotesk_bold.ttf"
Invoke-WebRequest -Uri "https://github.com/JetBrains/JetBrainsMono/raw/master/fonts/ttf/JetBrainsMono-Regular.ttf" -OutFile "app\src\main\res\font\jetbrains_mono_regular.ttf"
Invoke-WebRequest -Uri "https://github.com/JetBrains/JetBrainsMono/raw/master/fonts/ttf/JetBrainsMono-Medium.ttf" -OutFile "app\src\main\res\font\jetbrains_mono_medium.ttf"
Get-ChildItem "app\src\main\res\font"
```

- [ ] **Step 3: Verifikasi**

Run: `Get-ChildItem "app\src\main\res\font" | Select-Object Name, Length`
Expected: 3 file, masing-masing > 50 KB (TTF asli). Jika salah satu 404/kosong → hentikan, laporkan, jangan lanjut.

- [ ] **Step 4: Catat lisensi (OFL)**

Buat `app/src/main/res/raw/font_licenses.txt`:

```
OMNIX OS bundled fonts — all under SIL Open Font License 1.1:
- Space Grotesk (c) Florian Karsten — https://github.com/floriankarsten/space-grotesk
- JetBrains Mono (c) JetBrains — https://github.com/JetBrains/JetBrainsMono
```

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/res/font app/src/main/res/raw
git commit -m "feat(ui): bundle Space Grotesk & JetBrains Mono fonts (OFL)"
```

---

### Task 2: OmnixColors.kt — token warna semantik

**Files:**
- Create: `app/src/main/java/com/optimizer/android/ui/theme/OmnixColors.kt`

**Interfaces:**
- Consumes: tidak ada.
- Produces: `FeatureType { CLEANER, SHIELD, VAULT, CAMERA, STUDIO }`, `OmnixColorScheme` (field `base/ink/grid/danger: Color`, `fun accent(feature: FeatureType): Color`), `fun omnixColorScheme(): OmnixColorScheme`, `val LocalOmnixColors: CompositionLocal<OmnixColorScheme>` (dipakai Task 4–10).

- [ ] **Step 1: Tulis file**

```kotlin
package com.optimizer.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class FeatureType { CLEANER, SHIELD, VAULT, CAMERA, STUDIO }

@Immutable
data class OmnixColorScheme(
    val base: Color,
    val ink: Color,
    val grid: Color,
    val danger: Color,
    private val accentMap: Map<FeatureType, Color>
) {
    fun accent(feature: FeatureType): Color = accentMap.getValue(feature)
}

fun omnixColorScheme(): OmnixColorScheme = OmnixColorScheme(
    base = Color(0xFF0A0A0A),   // Base  — hitam diangkat
    ink = Color(0xFFF2F2F2),    // Ink   — teks utama off-white
    grid = Color(0xFF7A7A7A),   // Grid  — teks sekunder / label mono
    danger = Color(0xFFFF3B30), // Danger — alert
    accentMap = mapOf(
        FeatureType.CLEANER to Color(0xFFC6FF00), // Volt Lime
        FeatureType.SHIELD  to Color(0xFF00E5FF), // Cyan
        FeatureType.VAULT   to Color(0xFFB388FF), // Violet
        FeatureType.CAMERA  to Color(0xFFFF4D9D), // Magenta
        FeatureType.STUDIO  to Color(0xFFFF8A00)  // Orange
    )
)

val LocalOmnixColors = staticCompositionLocalOf { omnixColorScheme() }
```

- [ ] **Step 2: Verifikasi API**

Checklist (BOM 2023.08.00): `@Immutable`, `staticCompositionLocalOf`, `staticCompositionLocalOf { default }` valid. Tidak ada API Compose animasi di file ini.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/optimizer/android/ui/theme/OmnixColors.kt
git commit -m "feat(ui): add OmnixColors semantic color tokens"
```

---

### Task 3: OmnixMotion.kt + OmnixHaptics.kt

**Files:**
- Create: `app/src/main/java/com/optimizer/android/ui/theme/OmnixMotion.kt`
- Create: `app/src/main/java/com/optimizer/android/ui/theme/OmnixHaptics.kt`

**Interfaces:**
- Consumes: tidak ada.
- Produces:
  - `object OmnixMotion` dengan `INSTANT/QUICK/STANDARD/EMPHASIS: Int` (90/150/250/350), `STAGGER_MS = 40L`, `val snapSpring: SpringSpec<Float>`, `fun <T> quick(): TweenSpec<T>`, `fun <T> standard(): TweenSpec<T>`, `fun <T> emphasis(): TweenSpec<T>`, `val pressedScale = 0.97f`
  - `object OmnixHaptics` dengan `fun tick(h)`, `suspend fun doubleTick(h)`, `fun buzz(h)` (dipakai Task 5–7, 10)

- [ ] **Step 1: Tulis OmnixMotion.kt**

```kotlin
package com.optimizer.android.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object OmnixMotion {
    // Durasi (ms) — aturan anti-slop: semua animasi 90-350ms
    const val INSTANT = 90
    const val QUICK = 150
    const val STANDARD = 250
    const val EMPHASIS = 350
    const val STAGGER_MS = 40L

    val ambient: CubicBezierEasing = FastOutSlowInEasing
    val linear: LinearEasing = LinearEasing

    fun <T> quick(): TweenSpec<T> = tween(QUICK, easing = ambient)
    fun <T> standard(): TweenSpec<T> = tween(STANDARD, easing = ambient)
    fun <T> emphasis(): TweenSpec<T> = tween(EMPHASIS, easing = ambient)

    // "Menghentak" — snap balik dengan sedikit bouncy, ini rasa brutalism-nya
    val snapSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness = 500f
    )
    val dialogSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 400f
    )

    const val pressedScale = 0.97f
}
```

- [ ] **Step 2: Tulis OmnixHaptics.kt**

```kotlin
package com.optimizer.android.ui.theme

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay

object OmnixHaptics {
    // Tap kartu / toggle — satu tick ringan
    fun tick(h: HapticFeedback) {
        h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // Konfirmasi dialog sukses — double tick
    suspend fun doubleTick(h: HapticFeedback) {
        h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        delay(80)
        h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // Alert pertama muncul — satu buzz panjang ringan
    fun buzz(h: HapticFeedback) {
        h.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}
```

- [ ] **Step 3: Verifikasi API**

Checklist: `spring(dampingRatio, stiffness)` dan `tween(millis, easing)` dari `androidx.compose.animation.core` tersedia di 1.5.0. `HapticFeedbackType.TextHandleMove` & `LongPress` tersedia.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/optimizer/android/ui/theme/OmnixMotion.kt app/src/main/java/com/optimizer/android/ui/theme/OmnixHaptics.kt
git commit -m "feat(ui): add OmnixMotion + OmnixHaptics tokens"
```

---

### Task 4: OmnixTypography.kt + OmnixTheme.kt

**Files:**
- Create: `app/src/main/java/com/optimizer/android/ui/theme/OmnixTypography.kt`
- Create: `app/src/main/java/com/optimizer/android/ui/theme/OmnixTheme.kt`

**Interfaces:**
- Consumes: `R.font.space_grotesk_bold`, `R.font.jetbrains_mono_regular`, `R.font.jetbrains_mono_medium` (Task 1); `OmnixColorScheme`, `LocalOmnixColors` (Task 2).
- Produces:
  - `val SpaceGrotesk: FontFamily`, `val JetBrainsMono: FontFamily`
  - `object OmnixType` dengan `display/title/body/label/mono/valueBig: TextStyle`
  - `@Composable fun OmnixTheme(content)` — entry point (Task 10)
  - `object OmnixThemeColors { val colors: OmnixColorScheme @Composable get() }` (dipakai semua komponen)

- [ ] **Step 1: Tulis OmnixTypography.kt**

```kotlin
package com.optimizer.android.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.optimizer.android.R

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_bold, FontWeight.Bold)
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium)
)

object OmnixType {
    // Display 34sp Bold + letterspacing (judul besar "OMNIX OS")
    val display = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        letterSpacing = 2.sp
    )
    // Title 20sp Bold
    val title = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )
    // Body 14sp
    val body = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    )
    // Label 11sp Mono uppercase + letterspacing 1.5
    val label = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp
    )
    // Log/data teknis 12sp Mono
    val mono = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )
    // Angka status besar
    val valueBig = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    )
}
```

- [ ] **Step 2: Tulis OmnixTheme.kt**

```kotlin
package com.optimizer.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

private val omnixMaterialScheme = darkColorScheme(
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF0A0A0A),
    onBackground = Color(0xFFF2F2F2),
    onSurface = Color(0xFFF2F2F2),
    primary = Color(0xFFC6FF00),
    error = Color(0xFFFF3B30)
)

@Composable
fun OmnixTheme(content: @Composable () -> Unit) {
    val colors = remember { omnixColorScheme() }
    CompositionLocalProvider(LocalOmnixColors provides colors) {
        MaterialTheme(colorScheme = omnixMaterialScheme, content = content)
    }
}

object OmnixThemeColors {
    val colors: OmnixColorScheme
        @Composable get() = LocalOmnixColors.current
}
```

- [ ] **Step 3: Verifikasi**

Checklist: `darkColorScheme` material3 valid; `remember { omnixColorScheme() }` = 1 instance per komposisi; nama `OmnixThemeColors` TIDAK bentrok dengan composable `OmnixTheme` (beda nama, aman).

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/optimizer/android/ui/theme
git commit -m "feat(ui): add OmnixType typography + OmnixTheme wrapper"
```

---

### Task 5: StatusCard rewrite (count-up value, border accent, alert pulse + buzz)

**Files:**
- Modify: `app/src/main/java/com/optimizer/android/ui/components/OmnixComponents.kt` — ganti seluruh isi bagian atas (hapus 3 val warna lama `PitchBlack/CrispWhite/NeonGreen` **JANGAN dihapus dulu** — masih dipakai MainActivity; pindahkan ke bawah file sebagai deprecated alias agar MainActivity lama tetap compile. Ganti implementasi `StatusCard`).

**Interfaces:**
- Consumes: `OmnixThemeColors.colors`, `OmnixType`, `OmnixMotion`, `OmnixHaptics` (Task 2–4).
- Produces: `@Composable fun StatusCard(modifier: Modifier = Modifier, title: String, value: String, alert: Boolean = false, index: Int = 0)` — signatur kompatibel dengan pemanggilan lama di MainActivity (4 call sites), jadi commit ini tetap buildable.

- [ ] **Step 1: Tulis StaggerIn + StatusCard (ganti implementasi lama, pertahankan 3 val warna di bawah file)**

```kotlin
package com.optimizer.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.optimizer.android.ui.theme.OmnixMotion
import com.optimizer.android.ui.theme.OmnixThemeColors
import com.optimizer.android.ui.theme.OmnixHaptics
import com.optimizer.android.ui.theme.OmnixType

// Staggered entrance — fade + slide-up, jeda 40ms antar item
@Composable
fun StaggerIn(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // Di @Preview (inspection mode) langsung tampil, tidak menunggu animasi
    val inPreview = androidx.compose.ui.platform.LocalInspectionMode.current
    var entered by remember { mutableStateOf(inPreview) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * OmnixMotion.STAGGER_MS + 100L)
        entered = true
    }
    AnimatedVisibility(
        visible = entered,
        enter = fadeIn(OmnixMotion.standard()) + slideInVertically(OmnixMotion.standard()) { it / 3 },
        modifier = modifier
    ) { Box { content() } }
}

@Composable
fun StatusCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    alert: Boolean = false,
    index: Int = 0
) {
    val colors = OmnixThemeColors.colors
    val border = if (alert) colors.danger else colors.ink
    val alertPulse = rememberInfiniteTransition(label = "alertPulse")
    val pulseAlpha by alertPulse.animateFloat(
        initialValue = 1f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(2000, easing = OmnixMotion.ambient), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(alert) { if (alert) OmnixHaptics.buzz(haptics) }

    StaggerIn(index, modifier) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            border = BorderStroke(2.dp, border.copy(alpha = if (alert) pulseAlpha else 1f)),
            colors = CardDefaults.outlinedCardColors(containerColor = colors.base)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    title.uppercase(),
                    style = OmnixType.label,
                    color = if (alert) colors.danger else colors.grid
                )
                Spacer(Modifier.height(6.dp))
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (slideInVertically(OmnixMotion.standard()) { it } + fadeIn(OmnixMotion.standard())) togetherWith
                            (slideOutVertically(OmnixMotion.standard()) { -it } + fadeOut(OmnixMotion.standard()))
                    },
                    label = "statusValue"
                ) { v ->
                    Text(v, style = OmnixType.valueBig, color = if (alert) colors.danger else colors.ink)
                }
            }
        }
    }
}
```

- [ ] **Step 2: Pindahkan 3 val warna lama ke bawah file dengan komentar deprecated**

Di bagian paling bawah `OmnixComponents.kt` (sebelum `NeoDialog` lama), tambahkan:

```kotlin
// ---- DEPRECATED legacy color tokens (dihapus di Task 10) ----
val PitchBlack = Color(0xFF000000)
val CrispWhite = Color(0xFFFFFFFF)
val NeonGreen = Color(0xFF00FF00)
```

Dan hapus deklarasi lama di atas file. `NeoDialog` & `SuperpowerCard` lama tetap berisi implementasi lama (dirombak Task 6–7) — mereka masih mereferensikan `PitchBlack/CrispWhite`, jadi alias ini menjaga kompilasi.

- [ ] **Step 3: Verifikasi**

Run: `git diff --stat`
Checklist: `StatusCard` masih menerima pemanggilan lama `StatusCard(modifier = ..., title = ..., value = "...", alert = ...)`; `AnimatedContent` + `togetherWith` tersedia di animation 1.5.0; tidak ada hardcode warna baru di dalam komponen.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/optimizer/android/ui/components/OmnixComponents.kt
git commit -m "feat(ui): rewrite StatusCard with stagger entrance, value slide, alert pulse + haptics"
```

---

### Task 6: SuperpowerCard rewrite (accent rail, magnetic press, hard shadow, haptic)

**Files:**
- Modify: `app/src/main/java/com/optimizer/android/ui/components/OmnixComponents.kt` — ganti implementasi `SuperpowerCard`.

**Interfaces:**
- Consumes: token theme (Task 2–4), `StaggerIn` (Task 5).
- Produces: `@Composable fun SuperpowerCard(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Color? = null, index: Int = 0)` — `accent` default null → ink; posisi param `onClick` tetap ke-3, kompatibel dengan 8 call sites lama di MainActivity.

- [ ] **Step 1: Tulis implementasi baru**

```kotlin
@Composable
fun SuperpowerCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    index: Int = 0
) {
    val colors = OmnixThemeColors.colors
    val rail = accent ?: colors.ink
    val haptics = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    var pressX by remember { mutableStateOf(0.5f) }

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) OmnixMotion.pressedScale else 1f,
        animationSpec = OmnixMotion.snapSpring,
        label = "pressScale"
    )
    val shadowAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = OmnixMotion.quick(),
        label = "shadowAlpha"
    )

    StaggerIn(index, modifier.fillMaxWidth()) {
        Box(Modifier.padding(bottom = 8.dp)) {
            // Hard accent shadow (glow brutalism) — muncul saat pressed
            Box(
                Modifier
                    .matchParentSize()
                    .offset(x = 4.dp, y = 4.dp)
                    .background(rail)
                    .alpha(shadowAlpha)
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale; scaleY = scale
                        translationX = ((pressX - 0.5f) * 4f).dp.toPx()
                    }
                    .border(2.dp, colors.ink, RectangleShape)
                    .background(colors.base)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                pressX = (it.x / size.width.toFloat()).coerceIn(0f, 1f)
                                OmnixHaptics.tick(haptics)
                                tryAwaitRelease()
                                pressed = false
                            },
                            onTap = { onClick() }
                        )
                    }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .border(2.dp, colors.ink, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = rail)
                    }
                    Spacer(Modifier.width(14.dp))
                    // Accent rail kiri ikon
                    Box(Modifier.width(4.dp).height(28.dp).background(rail))
                    Spacer(Modifier.width(12.dp))
                    Text(title, style = OmnixType.title, color = colors.ink)
                }
            }
        }
    }
}
```

Tambahkan import yang belum ada di header file:

```kotlin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
```

- [ ] **Step 2: Verifikasi**

Checklist: `detectTapGestures` + `tryAwaitRelease()` dari `androidx.compose.foundation.gestures` valid; `size` di dalam `pointerInput` adalah `IntSize` area; kartu lama dengan pemanggilan `SuperpowerCard(title = ..., icon = ..., onClick = ...)` tetap compile (accent default null).

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/optimizer/android/ui/components/OmnixComponents.kt
git commit -m "feat(ui): rewrite SuperpowerCard with accent rail, magnetic press, hard shadow"
```

---

### Task 7: NeoDialog rewrite (pop-in spring, scrim fade, tombol aksen) + SectionHeader + BrutalBar

**Files:**
- Modify: `app/src/main/java/com/optimizer/android/ui/components/OmnixComponents.kt` — ganti implementasi `NeoDialog`, tambah `SectionHeader` + `BrutalBar`.

**Interfaces:**
- Consumes: token theme, `OmnixHaptics`.
- Produces:
  - `@Composable fun NeoDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier, accent: Color? = null, confirmLabel: String = "GASKAN", content: (@Composable () -> Unit)? = null)` — posisi `onConfirm/onDismiss` tetap 3–4, kompatibel dengan 4 call sites lama.
  - `@Composable fun SectionHeader(number: String, title: String, modifier: Modifier = Modifier)`
  - `@Composable fun BrutalBar(progress: Float, accent: Color, modifier: Modifier = Modifier)`

- [ ] **Step 1: Ganti NeoDialog, tambah SectionHeader & BrutalBar**

```kotlin
@Composable
fun NeoDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    confirmLabel: String = "GASKAN",
    content: (@Composable () -> Unit)? = null
) {
    val colors = OmnixThemeColors.colors
    val accentColor = accent ?: colors.ink
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scrim by animateFloatAsState(
        targetValue = if (shown) 0.75f else 0f,
        animationSpec = OmnixMotion.quick(),
        label = "scrim"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.9f,
        animationSpec = OmnixMotion.dialogSpring,
        label = "dialogScale"
    )

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.base.copy(alpha = scrim))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { scaleX = contentScale; scaleY = contentScale }
                    .border(2.dp, colors.ink, RectangleShape)
                    .background(colors.base)
            ) {
                Box(Modifier.fillMaxWidth().height(6.dp).background(accentColor))
                Column(Modifier.padding(20.dp)) {
                    Text(title, style = OmnixType.title, color = colors.ink)
                    Spacer(Modifier.height(10.dp))
                    if (content != null) {
                        content()
                    } else {
                        Text(text, style = OmnixType.mono, color = colors.grid)
                    }
                    Spacer(Modifier.height(20.dp))
                    Row {
                        Button(
                            onClick = {
                                scope.launch { OmnixHaptics.doubleTick(haptics) }
                                onConfirm()
                            },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = colors.base)
                        ) { Text(confirmLabel, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RectangleShape,
                            border = BorderStroke(2.dp, colors.ink),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.ink)
                        ) { Text("BATAL", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(number: String, title: String, modifier: Modifier = Modifier) {
    val colors = OmnixThemeColors.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text("$number /", style = OmnixType.label, color = colors.grid)
        Spacer(Modifier.width(8.dp))
        Text(title.uppercase(), style = OmnixType.title, color = colors.ink)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f).height(2.dp).background(colors.ink))
    }
}

@Composable
fun BrutalBar(progress: Float, accent: Color, modifier: Modifier = Modifier) {
    val total = 16
    val filled = (progress.coerceIn(0f, 1f) * total).toInt()
    val bar = "█".repeat(filled) + "░".repeat(total - filled)
    Text(
        "$bar ${ (progress.coerceIn(0f, 1f) * 100).toInt() }%",
        style = OmnixType.mono,
        color = accent,
        modifier = modifier
    )
}
```

Tambah import yang belum ada:

```kotlin
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
```

Hapus `@OptIn(ExperimentalMaterial3Api::class)` yang tidak lagi terpakai di NeoDialog, dan hapus implementasi `AlertDialog` lama beserta import `androidx.compose.material3.AlertDialog` bila tidak terpakai (BLACKHOLE dialog di MainActivity masih memakai AlertDialog sendiri — import material3 tetap ada di MainActivity, bukan di sini).

- [ ] **Step 2: Verifikasi**

Checklist: `DialogProperties(usePlatformDefaultWidth = false)` valid; `rememberCoroutineScope` dari `androidx.compose.runtime` valid; pemanggilan lama `NeoDialog(title = ..., text = ..., onConfirm = ..., onDismiss = ...)` tetap compile.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/optimizer/android/ui/components/OmnixComponents.kt
git commit -m "feat(ui): rewrite NeoDialog with pop-in spring, add SectionHeader + BrutalBar"
```

---

### Task 8: TerminalLog (konsol hidup, empty state, blinking cursor)

**Files:**
- Create: `app/src/main/java/com/optimizer/android/ui/components/TerminalLog.kt`

**Interfaces:**
- Consumes: token theme, `OmnixType`.
- Produces: `@Composable fun TerminalLog(logs: List<String>, modifier: Modifier = Modifier)` (dipakai Task 10).

- [ ] **Step 1: Tulis file**

```kotlin
package com.optimizer.android.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.optimizer.android.ui.theme.OmnixThemeColors
import com.optimizer.android.ui.theme.OmnixType

@Composable
fun TerminalLog(logs: List<String>, modifier: Modifier = Modifier) {
    val colors = OmnixThemeColors.colors
    val blink = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by blink.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "cursorAlpha"
    )
    val visible = logs.takeLast(50) // limit 50 baris
    val listState = rememberLazyListState()

    LaunchedEffect(visible.size) {
        if (visible.isNotEmpty()) listState.animateScrollToItem(visible.lastIndex)
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(2.dp, colors.ink, RectangleShape)
            .background(colors.base)
            .padding(12.dp)
    ) {
        if (visible.isEmpty()) {
            // Empty state — bukan layar kosong
            androidx.compose.material3.Text(
                "> AWAITING SIGNAL...▮",
                style = OmnixType.mono,
                color = colors.grid.copy(alpha = 0.4f + 0.6f * cursorAlpha)
            )
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(
                    visible,
                    key = { i, s -> "$i-${s.hashCode()}" }
                ) { _, log ->
                    androidx.compose.material3.Text(
                        "> $log",
                        style = OmnixType.mono,
                        color = colors.ink,
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .animateItemPlacement()
                    )
                }
                item(key = "cursor") {
                    androidx.compose.material3.Text(
                        "> ▮",
                        style = OmnixType.mono,
                        color = colors.grid.copy(alpha = 0.2f + 0.8f * cursorAlpha)
                    )
                }
            }
        }
    }
}
```

**Catatan API (penting):** di Compose 1.5.0 (BOM 2023.08.00) modifier yang benar adalah `Modifier.animateItemPlacement()` dengan `@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)` — tambahkan `@OptIn` di atas fungsi `TerminalLog`. `Modifier.animateItem()` TIDAK tersedia di versi ini.

- [ ] **Step 2: Tambahkan OptIn annotation**

```kotlin
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TerminalLog(logs: List<String>, modifier: Modifier = Modifier) {
```

- [ ] **Step 3: Verifikasi**

Checklist: `animateItemPlacement()` (bukan `animateItem`) dari `androidx.compose.foundation.lazy` tersedia di foundation 1.5.0; key unik per item; empty state ada; cursor blink hanya 500ms loop.

- [ ] **Step 4: Buat Preview file (spec §9.1)**

Create `app/src/main/java/com/optimizer/android/ui/components/OmnixPreviews.kt`:

```kotlin
package com.optimizer.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.optimizer.android.ui.theme.FeatureType
import com.optimizer.android.ui.theme.OmnixTheme
import com.optimizer.android.ui.theme.OmnixThemeColors

@Preview(name = "StatusCards", showBackground = true, backgroundColor = 0xFF0A0A0A, widthDp = 360)
@Composable
private fun PreviewStatusCards() {
    OmnixTheme {
        Surface {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusCard(title = "STORAGE", value = "12480 MB")
                StatusCard(title = "TEMP", value = "43 °C", alert = true)
            }
        }
    }
}

@Preview(name = "SuperpowerCards", showBackground = true, backgroundColor = 0xFF0A0A0A, widthDp = 360)
@Composable
private fun PreviewSuperpowerCards() {
    OmnixTheme {
        Surface {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SuperpowerCard(
                    title = "JUNK & CACHE CLEANER",
                    icon = Icons.Filled.CleaningServices,
                    onClick = {},
                    accent = OmnixThemeColors.colors.accent(FeatureType.CLEANER)
                )
                SuperpowerCard(title = "WORK PROFILE ENGINE", icon = Icons.Filled.CleaningServices, onClick = {})
            }
        }
    }
}

@Preview(name = "SectionHeader + BrutalBar", showBackground = true, backgroundColor = 0xFF0A0A0A, widthDp = 360)
@Composable
private fun PreviewHeaderBar() {
    OmnixTheme {
        Surface {
            Column(Modifier.padding(8.dp)) {
                SectionHeader("01", "SYSTEM STATUS")
                BrutalBar(0.62f, OmnixThemeColors.colors.accent(FeatureType.CLEANER))
            }
        }
    }
}

@Preview(name = "TerminalLog filled", showBackground = true, backgroundColor = 0xFF0A0A0A, widthDp = 360)
@Composable
private fun PreviewTerminalLog() {
    OmnixTheme {
        TerminalLog(logs = listOf("BOOT SISTEM BERHASIL.", "MEMULAI SCANNING...", "12 BERKAS DIHAPUS."))
    }
}

@Preview(name = "TerminalLog empty", showBackground = true, backgroundColor = 0xFF0A0A0A, widthDp = 360)
@Composable
private fun PreviewTerminalLogEmpty() {
    OmnixTheme {
        TerminalLog(logs = emptyList())
    }
}
```

Catatan: `NeoDialog` sengaja tidak di-preview (Dialog tidak dirender di Preview tool).

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/optimizer/android/ui/components/TerminalLog.kt app/src/main/java/com/optimizer/android/ui/components/OmnixPreviews.kt
git commit -m "feat(ui): add TerminalLog + component previews"
```

**Milestone CI #1 (opsional tapi disarankan):** push branch & dispatch workflow untuk memverifikasi semua file baru ter-compile:

```powershell
git push -u origin feat/omnix-design-system
```

Lalu trigger workflow `android-build.yml` pada ref `feat/omnix-design-system` (via `gh workflow run android-build.yml --ref feat/omnix-design-system` atau GitHub MCP `run_workflow`), tunggu hasil `success`. Jika merah: perbaiki, commit fix, dispatch ulang.

---

### Task 9: MainViewModel — tambah scanProgress

**Files:**
- Modify: `app/src/main/java/com/optimizer/android/presentation/MainViewModel.kt` (line ~21–30 data class, line ~69–87 startJunkScan)

**Interfaces:**
- Consumes: tidak berubah.
- Produces: `MainUiState.scanProgress: Float` (0f..1f) — dipakai Task 10 untuk `BrutalBar`.

- [ ] **Step 1: Tambahkan field di MainUiState**

```kotlin
data class MainUiState(
    val storageStat: StorageStatus = StorageStatus(0, 0),
    val ramStat: RamStatus = RamStatus(0, 0),
    val batteryStat: BatteryStatus = BatteryStatus(0f, BatteryHealth.UNKNOWN),
    val logs: List<String> = listOf("BOOT SISTEM BERHASIL."),
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val activeDialog: DialogType = DialogType.NONE,
    val vpnActive: Boolean = false,
    val vaultContent: String = "BRANKAS KOSONG."
)
```

- [ ] **Step 2: Update startJunkScan**

```kotlin
fun startJunkScan() {
    viewModelScope.launch {
        _uiState.update { it.copy(isScanning = true, scanProgress = 0f, activeDialog = DialogType.NONE) }
        log("MEMULAI SCANNING SAMPAH & CACHE...")

        systemRepository.scanJunk().collect { msg ->
            log(msg)
            _uiState.update { it.copy(scanProgress = (it.scanProgress + 0.05f).coerceAtMost(0.95f)) }
        }
        val found = systemRepository.getJunkFiles()

        if (found.isNotEmpty()) {
            log("MEMBERSIHKAN ${found.size} BERKAS SAMPAH...")
            systemRepository.deleteJunkFiles(found).collect { msg ->
                log(msg)
                _uiState.update { it.copy(scanProgress = (it.scanProgress + 0.05f).coerceAtMost(0.95f)) }
            }
            refreshSystemStatus()
            log("CLEANUP SELESAI! PEMBERSIHAN SUKSES.")
        } else {
            log("PENYIMPANAN SUDAH BERSIH.")
        }
        _uiState.update { it.copy(scanProgress = 1f, isScanning = false) }
    }
}
```

- [ ] **Step 3: Verifikasi**

Checklist: hanya field + 2 titik update; tidak ada perubahan alur lain; default `scanProgress = 0f` tidak merusak pemanggil lama.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/optimizer/android/presentation/MainViewModel.kt
git commit -m "feat(ui): add scanProgress to MainUiState for brutal progress bar"
```

---

### Task 10: MainActivity integrasi penuh

**Files:**
- Modify: `app/src/main/java/com/optimizer/android/MainActivity.kt`

**Interfaces:**
- Consumes: `OmnixTheme`, `OmnixThemeColors`, semua komponen Task 5–8, `MainUiState.scanProgress` (Task 9).
- Produces: UI dashboard final. Tidak ada export.

- [ ] **Step 1: Ganti blok theme di `onCreate`**

```kotlin
setContent {
    com.optimizer.android.ui.theme.OmnixTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val uiState by viewModel.uiState.collectAsState()
            OptimizerDashboard(uiState)
        }
    }
}
```

- [ ] **Step 2: Tulis ulang `OptimizerDashboard`**

Struktur section dengan nomor mono; import komponen baru; hapus import warna lama (`CrispWhite/NeonGreen/PitchBlack/StatusCard lama` dari components — tetap boleh import komponen yang sama).

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizerDashboard(uiState: MainUiState) {
    val colors = com.optimizer.android.ui.theme.OmnixThemeColors.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            "OMNIX OS",
            style = com.optimizer.android.ui.theme.OmnixType.display,
            color = colors.ink
        )
        Text(
            "GOD-TIER SUPERAPP • NO ROOT REQUIRED",
            style = com.optimizer.android.ui.theme.OmnixType.label,
            color = colors.grid
        )

        // 01 / SYSTEM STATUS
        SectionHeader("01", "SYSTEM STATUS")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusCard(Modifier.weight(1f), "STORAGE", "${uiState.storageStat.freeMb} MB", index = 1)
            StatusCard(Modifier.weight(1f), "RAM", "${uiState.ramStat.freeMb} MB", index = 2)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusCard(
                Modifier.weight(1f), "TEMP",
                "${uiState.batteryStat.tempCelsius} °C",
                alert = uiState.batteryStat.tempCelsius > 40f, index = 3
            )
            StatusCard(
                Modifier.weight(1f), "HEALTH",
                uiState.batteryStat.health.name,
                alert = uiState.batteryStat.health.name != "GOOD", index = 4
            )
        }

        // 02 / CORE OPTIMIZERS
        SectionHeader("02", "CORE OPTIMIZERS")
        SuperpowerCard(
            title = "JUNK & CACHE CLEANER (2-STAGE)",
            icon = Icons.Filled.CleaningServices,
            onClick = { viewModel.toggleDialog(DialogType.JUNK) },
            accent = colors.accent(com.optimizer.android.ui.theme.FeatureType.CLEANER),
            index = 5
        )
        androidx.compose.animation.AnimatedVisibility(
            visible = uiState.isScanning,
            enter = androidx.compose.animation.expandVertically(),
            exit = androidx.compose.animation.shrinkVertically()
        ) {
            com.optimizer.android.ui.components.BrutalBar(
                progress = uiState.scanProgress,
                accent = colors.accent(com.optimizer.android.ui.theme.FeatureType.CLEANER),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        SuperpowerCard(
            title = "RAM SPEED BOOSTER",
            icon = Icons.Filled.Speed,
            onClick = { viewModel.toggleDialog(DialogType.RAM) },
            accent = colors.accent(com.optimizer.android.ui.theme.FeatureType.CLEANER),
            index = 6
        )

        // 03 / SUPERPOWERS
        SectionHeader("03", "SUPERPOWERS")
        SuperpowerCard(
            title = "DEEP ROOT APP ERASER",
            icon = Icons.Filled.Delete,
            onClick = { startActivity(Intent(this@MainActivity, AppEraserActivity::class.java)) },
            accent = colors.accent(com.optimizer.android.ui.theme.FeatureType.CLEANER),
            index = 7
        )
        SuperpowerCard(
            title = "WORK PROFILE ENGINE",
            icon = Icons.Filled.FolderSpecial,
            onClick = { viewModel.toggleDialog(DialogType.HIBERNATION) },
            index = 8 // accent default = ink (netral sistem)
        )
        SuperpowerCard(
            title = "ANTI-DELETE MESSAGE VAULT",
            icon = Icons.Filled.Message,
            onClick = {
                viewModel.loadVaultContent()
                viewModel.toggleDialog(DialogType.BLACKHOLE)
            },
            accent = colors.accent(com.optimizer.android.ui.theme.FeatureType.VAULT),
            index = 9
        )
        SuperpowerCard(
            title = "DNS-LEVEL WEB SHIELD",
            icon = Icons.Filled.CloudOff,
            onClick = { viewModel.toggleDialog(DialogType.VPN) },
            accent = colors.accent(com.optimizer.android.ui.theme.FeatureType.SHIELD),
            index = 10
        )

        // 04 / PRO FEATURES
        SectionHeader("04", "PRO FEATURES")
        SuperpowerCard(
            title = "iCLONE PRO CAMERA",
            icon = Icons.Filled.CameraAlt,
            onClick = { startActivity(Intent(this@MainActivity, ProCameraActivity::class.java)) },
            accent = colors.accent(com.optimizer.android.ui.theme.FeatureType.CAMERA),
            index = 11
        )
        SuperpowerCard(
            title = "PRO STUDIO AI (EDITOR)",
            icon = Icons.Filled.MovieCreation,
            onClick = { startActivity(Intent(this@MainActivity, ProStudioActivity::class.java)) },
            accent = colors.accent(com.optimizer.android.ui.theme.FeatureType.STUDIO),
            index = 12
        )

        // 05 / SYSTEM LOGS
        SectionHeader("05", "SYSTEM LOGS")
        com.optimizer.android.ui.components.TerminalLog(logs = uiState.logs)

        // LICENSE (dipertahankan, styling tetap brutalism)
        Spacer(Modifier.height(16.dp))
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.ui.graphics.RectangleShape,
            border = BorderStroke(2.dp, colors.ink),
            colors = CardDefaults.outlinedCardColors(containerColor = colors.base)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("LICENSE & COPYRIGHT", style = com.optimizer.android.ui.theme.OmnixType.title, color = colors.ink)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "© 2026 vickymosafan. All Rights Reserved.\n\n" +
                        "This software (OMNIX OS) and its God-Tier Superpowers (Work Profile Engine, Anti-Delete Vault, DNS Web Shield, iClone Pro Camera, Pro Studio AI) are the exclusive intellectual property of vickymosafan.\n\n" +
                        "Unauthorized copying, modification, distribution, or use of this software without explicit permission is strictly prohibited.",
                    style = com.optimizer.android.ui.theme.OmnixType.mono,
                    color = colors.grid
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // --- DIALOGS (accent per fitur) ---
    val cleanAccent = colors.accent(com.optimizer.android.ui.theme.FeatureType.CLEANER)
    val shieldAccent = colors.accent(com.optimizer.android.ui.theme.FeatureType.SHIELD)
    val vaultAccent = colors.accent(com.optimizer.android.ui.theme.FeatureType.VAULT)

    if (uiState.activeDialog == DialogType.VPN) {
        NeoDialog(
            title = "AKTIFKAN DNS SHIELD?",
            text = "Sistem akan mengaktifkan VPN Lokal untuk memblokir seluruh iklan dan situs kotor se-sistem via AdGuard DNS.",
            onConfirm = { viewModel.toggleDialog(DialogType.NONE); requestVpn() },
            onDismiss = { viewModel.toggleDialog(DialogType.NONE) },
            accent = shieldAccent
        )
    }
    if (uiState.activeDialog == DialogType.JUNK) {
        NeoDialog(
            title = "PINDAI & HAPUS CACHE SAMPAH?",
            text = "Sistem akan memindai berkas .tmp, .log, dan cache sisa aplikasi secara transparan tanpa merusak data penting Anda.",
            onConfirm = { viewModel.startJunkScan() },
            onDismiss = { viewModel.toggleDialog(DialogType.NONE) },
            accent = cleanAccent
        )
    }
    if (uiState.activeDialog == DialogType.RAM) {
        NeoDialog(
            title = "BOOST RAM & PERFORMANCE?",
            text = "Sistem akan memicu pembersihan alokasi memori latar belakang dan mengoptimalkan kecepatan RAM.",
            onConfirm = { viewModel.boostRam() },
            onDismiss = { viewModel.toggleDialog(DialogType.NONE) },
            accent = cleanAccent
        )
    }
    if (uiState.activeDialog == DialogType.HIBERNATION) {
        NeoDialog(
            title = "CREATE WORK PROFILE?",
            text = "Sistem akan membuat ruang ganda terpisah untuk mengkloning aplikasi.",
            onConfirm = { viewModel.toggleDialog(DialogType.NONE); requestWorkProfile() },
            onDismiss = { viewModel.toggleDialog(DialogType.NONE) }
        )
    }
    if (uiState.activeDialog == DialogType.BLACKHOLE) {
        NeoDialog(
            title = "ANTI-DELETE VAULT",
            text = "",
            onConfirm = { viewModel.toggleDialog(DialogType.NONE); requestNotificationAccess() },
            onDismiss = { viewModel.toggleDialog(DialogType.NONE) },
            accent = vaultAccent,
            confirmLabel = "ENABLE INTERCEPTOR",
            content = {
                Text(
                    uiState.vaultContent,
                    style = com.optimizer.android.ui.theme.OmnixType.mono,
                    color = colors.ink,
                    modifier = Modifier.heightIn(max = 300.dp)
                )
            }
        )
    }
}
```

- [ ] **Step 3: Bersihkan sisa token lama**

- Hapus deklarasi deprecated `PitchBlack/CrispWhite/NeonGreen` di `OmnixComponents.kt` (dari Task 5).
- Hapus import dari `com.optimizer.android.ui.components.PitchBlack/CrispWhite/NeonGreen` di MainActivity.
- Grep verifikasi: `grep -rn "NeonGreen\|PitchBlack\|CrispWhite" app/src/main/java` → harus 0 hasil (kecuali file layar lain jika ternyata memakai — jika ada, biarkan dan catat untuk polish berikutnya).

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/optimizer/android/MainActivity.kt app/src/main/java/com/optimizer/android/ui/components/OmnixComponents.kt
git commit -m "feat(ui): integrate OmnixTheme into dashboard with sectioned layout, accents, terminal log"
```

---

### Task 11: Push, CI green, merge

**Files:**
- Tidak ada file baru.

- [ ] **Step 1: Push branch**

```powershell
git push -u origin feat/omnix-design-system
```

- [ ] **Step 2: Trigger & pantau CI pada branch**

Via GitHub MCP: `run_workflow(workflow_id: "android-build.yml", ref: "feat/omnix-design-system")`, lalu `list_workflow_runs` → tunggu `completed: success`.
Atau via gh CLI: `gh run watch $(gh run list --workflow=android-build.yml --branch feat/omnix-design-system --limit 1 --json databaseId -q ".[0].databaseId")`

- [ ] **Step 3: Jika merah → perbaiki**

Baca log job (`get_job_logs`), perbaiki, commit `fix(ui): ...`, push ulang, dispatch ulang. Ulangi sampai hijau.

- [ ] **Step 4: Merge ke main**

```powershell
git checkout main
git merge --no-ff feat/omnix-design-system -m "merge: omnix ui design system (multi-accent brutalism, motion + haptics)"
git push origin main
```

- [ ] **Step 5: Verifikasi CI otomatis di main**

Push ke `main` otomatis trigger workflow. Pantau sampai `success` + artifact `Optimizer-App-APK` tersedia.

- [ ] **Step 6: QA checklist manual (laporkan ke user, butuh device/emulator)**

- [ ] Staggered entrance terasa saat buka app
- [ ] Magnetic press + haptic tick pada kartu fitur
- [ ] Dialog pop-in spring + tombol berwarna aksen fitur
- [ ] Alert TEMP/HEALTH memicu pulse merah + buzz
- [ ] Log baru slide-in di TerminalLog, cursor berkedip
- [ ] BrutalBar muncul saat scan, hilang setelah selesai

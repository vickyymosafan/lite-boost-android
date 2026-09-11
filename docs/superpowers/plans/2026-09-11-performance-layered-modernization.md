# Performance Layered Modernization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade stack (Kotlin 2.0/Compose modern/AGP 8.5), rilis R8, baseline profile + benchmark di CI, dan fix hotspot terukur agar OMNIX OS lancar di HP RAM kecil.

**Architecture:** 3 tuas resmi (upgrade compiler, R8 release, baseline profile) + fix hotspot teridentifikasi (parse lists async, DNS forward async, pulse kondisional, remember di composition, lazy worker schedule).

**Tech Stack:** Kotlin 2.0.21, Compose BOM 2024.09.03, AGP 8.5.2, Gradle 8.7, Hilt 2.52 (kapt), WorkManager 2.9.1, CameraX 1.4.0, Media3 1.4.1, macrobenchmark 1.3.0, baseline-profile plugin 1.3.1, profileinstaller 1.3.1.

## Global Constraints

- **Verifikasi = GitHub Actions** (`android-build.yml`: `gradle assembleDebug`; setelah Task 2: BOTH `assembleDebug` dan `assembleRelease`). Tidak ada build lokal. Iteration policy user: CI merah → fix → push ulang, JANGAN berhenti.
- Versi pin sesuai Tech Stack di atas. **Fallback resmi** bila CI merah karena upgrade (task 1): Kotlin 1.9.24 + Compose BOM 2024.06.00 + AGP 8.4.2 (tanpa strong skipping) — hanya setelah 2 percobaan fix pada 2.0.21 gagal; catat keputusan di report.
- Gradle wrapper: workflow memakai `gradle-version: '8.7'` (AGP 8.5.2 butuh Gradle ≥8.7). Jangan commit wrapper.
- Jangan ubah file fitur (UI/logika) pada Task 1–2 selain yang diperlukan upgrade.
- Setiap commit individually buildable. Commit terpisah per task. Branch: `feat/perf-modernization`.
- Baseline infra deviation (disetujui di plan): plugin `androidx.baselineprofile` butuh AGP ≥8.2 → infra+baseline capture dilakukan SETELAH Task 1–2 (upgrade+R8), SEBELUM Task 4 (hotspot). Intent spec "ukur sebelum optimasi" tetap terpenuhi.
- Spec: `docs/superpowers/specs/2026-09-11-performance-layered-modernization-design.md`.
- GMD/benchmark hanya lewat workflow manual `benchmark.yml` (bukan tiap push).

## File Structure

```
gradle.properties / build.gradle.kts / app/build.gradle.kts / settings.gradle.kts  (Task 1, 3)
app/proguard-rules.pro                                                           (Task 2)
.github/workflows/android-build.yml                                              (Task 2)
.github/workflows/benchmark.yml + baselineprofile/ (module) + app GMD config     (Task 3)
data/repository/DnsShieldRepositoryImpl.kt (+interface + VM + ShieldActivity)    (Task 4)
LocalFirewallService.kt                                                          (Task 5)
ui/components/OmnixComponents.kt, TerminalLog.kt, ShieldActivity.kt, OmnixApplication.kt, MainActivity.kt (Task 6)
app/src/main/baselineProfiles/*                                                  (Task 7)
```

---

### Task 1: Branch + upgrade stack (Kotlin 2.0.21 / Compose BOM 2024.09.03 / AGP 8.5.2)

**Files:**
- Modify: `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties`, `.github/workflows/android-build.yml` (gradle-version 8.7)

**Interfaces:**
- Produces: stack baru yang dipakai semua task berikutnya. Perubahan API yang WAJIB ditangani di task ini bila compiler mengeluh: `kotlinOptions` → `compilerOptions` (opsional, masih jalan), `composeOptions` DIHAPUS, plugin compose ditambahkan.

- [ ] **Step 1: Branch**

```powershell
git checkout -b feat/perf-modernization
```

- [ ] **Step 2: Root `build.gradle.kts`** — ganti isi:

```kotlin
// Top-level build file
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.dagger.hilt.android") version "2.52" apply false
}
```

- [ ] **Step 3: `app/build.gradle.kts`** — plugins + android block + deps:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}
```
HAPUS blok `composeOptions { kotlinCompilerExtensionVersion = "1.4.3" }` (digantikan plugin). Sisanya tetap; dependency jadi:

```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")

    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    val cameraxVersion = "1.4.0"
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-video:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")
    implementation("androidx.camera:camera-extensions:${cameraxVersion}")

    val media3Version = "1.4.1"
    implementation("androidx.media3:media3-common:$media3Version")
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-transformer:$media3Version")
    implementation("androidx.media3:media3-effect:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")

    testImplementation("junit:junit:4.13.2")
}
```

- [ ] **Step 4: `gradle.properties`** — pastikan/tambah:

```properties
android.useAndroidX=true
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
kotlin.code.style=official
```

- [ ] **Step 5: Workflow** — `.github/workflows/android-build.yml`: ganti `gradle-version: '8.2'` → `'8.7'`; step build jadi:

```yaml
    - name: Build APK (Debug + Release)
      run: gradle assembleDebug assembleRelease
```

- [ ] **Step 6: API-migration fixes (hanya jika CI menemukan)** — kemungkinan: `LinearEasing`/`animateItemPlacement` deprecated → ganti `Modifier.animateItem()` (BOM baru) di TerminalLog (hapus @OptIn ExperimentalFoundationApi); `mutableIntStateOf` tetap valid. JANGAN refactor lain. Iterasi sampai CI hijau.

- [ ] **Step 7: Commit**

```powershell
git add build.gradle.kts app/build.gradle.kts gradle.properties .github/workflows/android-build.yml
git commit -m "build: upgrade to Kotlin 2.0.21 / Compose BOM 2024.09.03 / AGP 8.5.2 (K2 + current compose compiler plugin)"
```

---

### Task 2: Rilis R8 (sign debug-key) + artifact release

**Files:**
- Modify: `app/build.gradle.kts` (buildTypes.release + signingConfigs), `app/proguard-rules.pro`, `.github/workflows/android-build.yml` (artifact path)

**Interfaces:** Produces: APK release R8 di CI artifact `Optimizer-App-APK` (path `app/build/outputs/apk/release/app-release.apk`).

- [ ] **Step 1: `app/build.gradle.kts`** — signing + release:

```kotlin
android {
    signingConfigs {
        getByName("debug") // default debug keystore dipakai untuk release
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
```

- [ ] **Step 2: `app/proguard-rules.pro`** — tambahkan keep rules:

```proguard
# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# WorkManager + HiltWorker
-keep class * extends androidx.work.ListenableWorker { public <init>(...); }
-keep @androidx.hilt.work.HiltWorker class * { *; }

# Komponen manifest (service/receiver)
-keep class com.optimizer.android.LocalFirewallService { *; }
-keep class com.optimizer.android.BlackholeNotificationService { *; }
-keep class com.optimizer.android.HibernationService { *; }
-keep class com.optimizer.android.WorkProfileReceiver { *; }
-keep class com.optimizer.android.PackageRemoveReceiver { *; }

# Media3 / CameraX reflection
-dontwarn androidx.media3.**
-keep class androidx.media3.common.util.** { *; }
-keep class androidx.camera.** { *; }

# Kotlin
-keepclassmembers class kotlin.Metadata { *; }
-dontwarn kotlin.**
```

- [ ] **Step 3: Workflow artifact** — ganti path artifact:

```yaml
    - name: Upload APK Artifact
      uses: actions/upload-artifact@v4
      with:
        name: Optimizer-App-APK
        path: app/build/outputs/apk/release/app-release.apk
```

- [ ] **Step 4: CI iterasi sampai hijau** (R8 + shrinkResources sering butuh keep rule tambahan — fix hanya keep-rule terkait).

- [ ] **Step 5: Commit**

```powershell
git add app/build.gradle.kts app/proguard-rules.pro .github/workflows/android-build.yml
git commit -m "build: release APK with R8 + resource shrinking (debug-signed)"
```

---

### Task 3: Baseline infra + capture baseline (deviation: setelah upgrade, sebelum hotspot)

**Files:**
- Modify: `settings.gradle.kts` (include `:baselineprofile`), `app/build.gradle.kts` (plugin `androidx.baselineprofile` + GMD `testOptions` + `buildTypes.benchmark`), `app/build.gradle.kts` deps (+`androidx.benchmark:benchmark-macro-junit4` di app? tidak — cukup di module)
- Create: `baselineprofile/build.gradle.kts`, `baselineprofile/src/main/java/com/optimizer/android/baselineprofile/BaselineProfileGenerator.kt`, `StartupBenchmark.kt`, `FrameTimingBenchmark.kt`
- Create: `.github/workflows/benchmark.yml`
- Create: `.superpowers/perf/baseline-before.md` (angka hasil) — TIDAK di-commit (git-ignored), cukup report + ringkasan commit message.

**Interfaces:**
- Produces: task CI `gradle :app:generateBaselineProfile` + `gradle :baselineprofile:pixel6Api34BenchmarkAndroidTest`; profil kelak di `app/src/main/baselineProfiles/`; angka "before" untuk Task 7.

- [ ] **Step 1: `settings.gradle.kts`** tambah `include(":baselineprofile")` di akhir.
- [ ] **Step 2: app plugin `id("androidx.baselineprofile") version "1.3.1"`** (root: apply false, app: applied) + `buildTypes { create("benchmark") { isDebuggable = true; signingConfig = signingConfigs.getByName("debug"); matchingFallbacks += listOf("release") } }` + GMD:

```kotlin
testOptions {
    managedDevices {
        localDevices {
            create("pixel6Api34") {
                device = "Pixel 6"
                apiLevel = 34
                systemImageSource = "aosp-atd"
            }
        }
    }
}
```

- [ ] **Step 3: `baselineprofile/build.gradle.kts`**:

```kotlin
plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.optimizer.android.baselineprofile"
    compileSdk = 34
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    defaultConfig {
        minSdk = 28
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
    buildTypes { create("benchmark") { isDebuggable = true } }
    testOptions.managedDevices.localDevices.create("pixel6Api34") {
        device = "Pixel 6"
        apiLevel = 34
        systemImageSource = "aosp-atd"
    }
}

androidComponents {
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmark"
    }
}

dependencies {
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.3.0")
}
```

- [ ] **Step 4: `BaselineProfileGenerator.kt`**:

```kotlin
package com.optimizer.android.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = "com.optimizer.android",
            maxIterations = 15,
            stableIterations = 3
        ) {
            startActivityAndWait()
            device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
            Thread.sleep(500)
            device.findObject(By.scrollable(true))?.fling(Direction.UP)
        }
    }
}
```

- [ ] **Step 5: `StartupBenchmark.kt`**:

```kotlin
package com.optimizer.android.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupWithProfile() = rule.measureRepeated(
        packageName = "com.optimizer.android",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
        startupMode = StartupMode.COLD,
        iterations = 5
    ) {
        startActivityAndWait()
    }

    @Test
    fun startupNoCompilation() = rule.measureRepeated(
        packageName = "com.optimizer.android",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.None(),
        startupMode = StartupMode.COLD,
        iterations = 5
    ) {
        startActivityAndWait()
    }
}
```

- [ ] **Step 6: `FrameTimingBenchmark.kt`**:

```kotlin
package com.optimizer.android.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FrameTimingBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun scrollDashboard() = rule.measureRepeated(
        packageName = "com.optimizer.android",
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 5
    ) {
        startActivityAndWait()
        repeat(3) {
            device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
            Thread.sleep(300)
            device.findObject(By.scrollable(true))?.fling(Direction.UP)
            Thread.sleep(300)
        }
    }
}
```

- [ ] **Step 7: `.github/workflows/benchmark.yml`** (manual only):

```yaml
name: Benchmark (manual)
on:
  workflow_dispatch:
jobs:
  benchmark:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'gradle'
      - uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.7'
      - name: Enable KVM
        run: |
          echo 'KERNEL=="kvm", GROUP="kvm", MODE="0666", OPTIONS+="static_node=kvm"' | sudo tee /etc/udev/rules.d/99-kvm4all.rules
          sudo udevadm control --reload-rules
          sudo udevadm trigger --name-match=kvm
      - name: Baseline profile + benchmarks
        run: gradle :app:generateBaselineProfile :baselineprofile:pixel6Api34BenchmarkAndroidTest
      - name: Upload benchmark results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: benchmark-results
          path: |
            baselineprofile/build/outputs/
            app/src/main/baselineProfiles/
```

- [ ] **Step 8: Push + dispatch workflow benchmark di branch; catat angka "before" (startup P50/P90, frame P90) + konfirmasi generator menulis profil. jika infra gagal karena versi plugin, fallback: naikkan plugin `androidx.baselineprofile` ke versi terbaru yang kompatibel AGP 8.5 (catat), atau turunkan pendekatan ke `profileinstaller` manual + benchmark tetap dari module (generator dilewati, dicatat sebagai gap).**

- [ ] **Step 9: Commit**

```powershell
git add settings.gradle.kts app/build.gradle.kts baselineprofile .github/workflows/benchmark.yml app/src/main/baselineProfiles
git commit -m "perf: add baseline profile generator + macrobenchmark infra (GMD), capture before numbers"
```

---

### Task 4: Shield repository — parse async + reload per-list + allowlist StateFlow

**Files:**
- Modify: `app/src/main/java/com/optimizer/android/data/repository/DnsShieldRepositoryImpl.kt`, `domain/repository/DnsShieldRepository.kt`, `presentation/ShieldViewModel.kt`, `presentation/ShieldActivity.kt`

**Interfaces:**
- Produces:
  - `DnsShieldRepository` tambah `val allowlist: StateFlow<Set<String>>` (menggantikan pola snapshot+tick di UI; `allowlistSnapshot()` tetap ada untuk kompatibilitas).
  - `reloadLists(changed: FilterListId? = null)` — parameter baru default null (reload semua hanya saat init/update; toggle mengirim id).
  - `val rulesReady: StateFlow<Boolean>` — false sampai parse pertama selesai.

- [ ] **Step 1: Impl — background parse.** Ganti `init { reloadLists() }` menjadi:

```kotlin
private val _rulesReady = MutableStateFlow(false)
override val rulesReady: StateFlow<Boolean> = _rulesReady.asStateFlow()

private val _allowlist = MutableStateFlow<Set<String>>(emptySet())
override val allowlist: StateFlow<Set<String>> = _allowlist.asStateFlow()

private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

init {
    engineScope.launch {
        reloadLists()
        _rulesReady.value = true
    }
}
```
`checkDomain` tetap aman sebelum siap (sets kosong → Allow). Sinkronkan `_allowlist.value` di `addAllowDomain`/`removeAllowDomain` (setelah `userAllow.add/remove`).

- [ ] **Step 2: `reloadLists(changed: FilterListId? = null)`** — hanya parse list yang diminta:

```kotlin
override fun reloadLists(changed: FilterListId?) {
    val targets = if (changed == null) FilterListId.values().toList() else listOf(changed)
    engineScope.launch {
        val statuses = _listStatuses.value.toMutableList()
        targets.forEach { id ->
            val enabled = listEnabled[id] == true
            if (!enabled) {
                blockedByList[id] = emptySet(); allowedByList[id] = emptySet()
                statuses.removeAll { it.id == id }
                statuses.add(FilterListStatus(id, false, 0))
                return@forEach
            }
            val content = store.readUpdated(id).takeIf { !it.isNullOrBlank() } ?: store.readBundled(id)
            val result = FilterListParser.parse(content)
            if (result.ruleCount < 100) {
                blockedByList[id] = emptySet(); allowedByList[id] = emptySet()
                statuses.removeAll { it.id == id }; statuses.add(FilterListStatus(id, true, 0))
            } else {
                blockedByList[id] = result.blocked
                allowedByList[id] = result.allowed
                statuses.removeAll { it.id == id }; statuses.add(FilterListStatus(id, true, result.ruleCount))
            }
        }
        _listStatuses.value = statuses.sortedBy { it.id.ordinal }
        _engineVersion.update { it + 1 }
    }
}
```
`setListEnabled` memanggil `reloadLists(id)` (bukan semua). `updateLists` sukses memanggil `reloadLists(null)`.

- [ ] **Step 3: Interface** — tambah `val allowlist: StateFlow<Set<String>>`, `val rulesReady: StateFlow<Boolean>`, ubah `fun reloadLists(changed: FilterListId? = null)`.

- [ ] **Step 4: ViewModel** — tambah `val allowlist = dnsShieldRepository.allowlist` + `val rulesReady = dnsShieldRepository.rulesReady`; `refreshLists()` → `dnsShieldRepository.reloadLists()`.

- [ ] **Step 5: ShieldActivity** — hapus `allowTick`/`allowlistSnapshot` hack; `val allowList by viewModel.allowlist.collectAsState()`; daftar rules menampilkan "rules: ..." atau placeholder loading bila `!rulesReady`; tambah `Modifier.verticalScroll(rememberScrollState())` pada Column utama (LazyColumn allowlist diganti `Column` biasa di dalam scroll — atau tetap LazyColumn dengan height tetap; pilih: `Column` biasa karena max ~20 item, hemat nested-scroll).

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/optimizer/android/data/repository/DnsShieldRepositoryImpl.kt app/src/main/java/com/optimizer/android/domain/repository/DnsShieldRepository.kt app/src/main/java/com/optimizer/android/presentation/ShieldViewModel.kt app/src/main/java/com/optimizer/android/presentation/ShieldActivity.kt
git commit -m "perf(shield): async list parsing, per-list reload, allowlist StateFlow, scrollable dashboard"
```

---

### Task 5: DNS engine — forward async (hilangkan stall)

**Files:**
- Modify: `app/src/main/java/com/optimizer/android/LocalFirewallService.kt`

**Interfaces:**
- Produces: engine tidak lagi memblokir loop saat forward; tetap single-writer TUN.

- [ ] **Step 1: Tambah field**:

```kotlin
private val writeMutex = kotlinx.coroutines.sync.Mutex()
private val inFlight = kotlinx.coroutines.sync.Semaphore(16)
```

- [ ] **Step 2: `processPacket`** — jalur Allow jadi async:

```kotlin
ShieldDecision.Allow -> {
    val pktRef = pkt
    val queryRef = query
    ioScope.launch {
        inFlight.withPermit {
            val upstream = forwarder.forward(pktRef.payload)
            val dnsPayload = upstream ?: DnsResponder.servfailResponse(queryRef)
            val response = IpPacket.buildUdp4Response(pktRef, dnsPayload)
            writeMutex.withLock { output.write(response) }
            dnsShieldRepository.incrementAllowed()
        }
    }
}
```
Jalur Block tetap sinkron + write lewat `writeMutex.withLock { output.write(...) }`.

- [ ] **Step 3: Catat di report** — forward tetap 5s timeout per query, tapi paralel maks 16 in-flight; engine loop tidak pernah menunggu jaringan.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/optimizer/android/LocalFirewallService.kt
git commit -m "perf(shield): async DNS forwarding with in-flight cap + single-writer TUN"
```

---

### Task 6: Compose hotspots + startup path

**Files:**
- Modify: `ui/components/OmnixComponents.kt` (StatusCard pulse), `ui/components/TerminalLog.kt` (remember + animateItem), `OmnixApplication.kt` + `MainActivity.kt` (worker schedule pindah), `presentation/MainViewModel.kt` (System.gc defer)

- [ ] **Step 1: StatusCard — pulse hanya saat alert.** Pisahkan border ke subkomposisi:

```kotlin
// Di dalam StatusCard, ganti pembacaan pulseAlpha selalu:
val border = if (alert) colors.danger else colors.ink
StaggerIn(index, modifier) {
    if (alert) {
        AlertPulseCard(title, value, border)   // punya rememberInfiniteTransition sendiri
    } else {
        StatusCardContent(title, value, border, alert = false, pulseAlpha = 1f)
    }
}
```
`AlertPulseCard` = komposisi 100% baru berisi `rememberInfiniteTransition`; `StatusCardContent` = isi kartu tanpa animator. Kartu non-alert tidak lagi menjalankan infinite animator.

- [ ] **Step 2: TerminalLog** — `remember` + `animateItem`:

```kotlin
val visible = remember(logs) { logs.takeLast(50) }
...
modifier = Modifier.padding(vertical = 2.dp).animateItem()
```
Hapus `@OptIn(ExperimentalFoundationApi::class)` (animateItem stabil di BOM baru). Hapus `animateItemPlacement`.

- [ ] **Step 3: Worker schedule pindah** — hapus `FilterListUpdateWorker.schedule(this)` dari `OmnixApplication.onCreate`; tambah di `MainActivity.onCreate` (setelah `super.onCreate`), import tetap. Alasan: hindari WorkManager disk-init di startup path.

- [ ] **Step 4: `boostRam` System.gc defer** — di `MainViewModel.boostRam()`:

```kotlin
viewModelScope.launch {
    _uiState.update { it.copy(activeDialog = DialogType.NONE) }
    log("MEMULAI OPTIMISASI RAM & GC...")
    kotlinx.coroutines.delay(200)
    withContext(Dispatchers.IO) { System.gc() }
    systemRepository.killBackgroundProcesses().collect { msg -> log(msg) }
    refreshSystemStatus()
    log("RAM OPTIMAL! RAM BEBAS: ${_uiState.value.ramStat.freeMb} MB")
}
```
(+ import `kotlinx.coroutines.withContext`, `kotlinx.coroutines.Dispatchers`.)

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/optimizer/android
git commit -m "perf(ui): conditional alert pulse, remembered log mapping, lazy worker schedule, deferred gc"
```

---

### Task 7: Generate & commit baseline profile + benchmark after

**Files:**
- Create/Commit: `app/src/main/baselineProfiles/*` (hasil generator)
- Commit message berisi ringkasan before/after.

- [ ] **Step 1:** Push branch; dispatch `benchmark.yml` di branch `feat/perf-modernization`.
- [ ] **Step 2:** Pastikan job sukses → file profil muncul (artifact + working tree di runner; download artifact, commit ke `app/src/main/baselineProfiles/` via lokal: `gh run download` atau copy manual dari artifact).
- [ ] **Step 3:** Jika startup `Partial+Require` vs `None` tidak lebih baik, catat gap; profil tetap di-commit bila generator sukses (profil tidak merugikan).
- [ ] **Step 4: Commit** — pesan commit HARUS memuat angka startup before/after aktual dari hasil benchmark (contoh format: `perf: add generated baseline profile (startup P50: 850ms -> 420ms)`)

```powershell
git add app/src/main/baselineProfiles
git commit -m "perf: add generated baseline profile (startup before/after: <angka aktual dari benchmark>)"
```

---

### Task 8: Final verify + merge

- [ ] **Step 1:** Push branch final; CI `android-build.yml` hijau (debug+release, artifact APK release).
- [ ] **Step 2:** Merge: `git checkout main && git merge --no-ff feat/perf-modernization -m "merge: performance layered modernization" && git push origin main`.
- [ ] **Step 3:** CI main hijau + artifact release tersedia.
- [ ] **Step 4:** QA device checklist (RAM kecil): startup; scroll dashboard; buka Shield tanpa jank; browse dengan shield; pause/resume; cleaner; exit 0 crash.
- [ ] **Step 5:** Laporkan angka before/after + gap (bila ada) ke user.

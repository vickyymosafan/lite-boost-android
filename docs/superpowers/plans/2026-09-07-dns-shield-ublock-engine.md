# DNS Shield uBlock-Style Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Engine filtering DNS lokal ala uBlock di dalam VpnService existing — narrow-route TUN, filter lists uAssets ter-bundel + auto-update, dashboard statistik ShieldActivity.

**Architecture:** Narrow-route DNS sinkhole (spec §4): TUN hanya menangkap paket DNS (routes /32 ke resolver populer), parser DNS minimal memutuskan block (0.0.0.0/::) atau forward ke AdGuard upstream via protected socket. Statistik + log via `DnsShieldRepository` (StateFlow), UI dashboard ala uBlock.

**Tech Stack:** Kotlin 1.8.10, Compose BOM 2023.08.00 (1.5.0), Hilt 2.48, WorkManager 2.8.1 + hilt-work, ParcelFileDescriptor TUN, HttpURLConnection.

## Global Constraints

- **Verifikasi = GitHub Actions** (`gradle assembleDebug`, workflow `android-build.yml`). Tidak ada build lokal. Setiap commit individually buildable.
- Compose 1.5.0: `Modifier.animateItemPlacement()` + `@OptIn(ExperimentalFoundationApi)`, BUKAN `animateItem()`.
- Kotlin 1.8.10: enum iteration pakai `.values()`, BUKAN `.entries` (stable mulai 1.9).
- `DnsForwarder` WAJIB memanggil `VpnService.protect(socket)` sebelum send — tanpa itu socket forwarder masuk lagi ke TUN (infinite loop).
- Komponen UI baru memakai token theme existing (`OmnixThemeColors.colors`, `OmnixType`, aksen `FeatureType.SHIELD`), komponen `StatusCard`/`SectionHeader`/`TerminalLog`/`SuperpowerCard` dari `ui/components`. Tidak ada hardcode warna.
- Bundle size lists: 3 file di `app/src/main/assets/filters/` — `easylist.txt`, `easyprivacy.txt`, `adguard_dns.txt` (Task 1 mengunduhnya).
- Palet aksen SHIELD = `#00E5FF` (sudah ada di OmnixColors — tidak menambah token baru).
- Pemanggil existing yang harus tetap jalan: `LocalFirewallService.startIntent/stopIntent` (dipakai MainActivity vpnLauncher), dialog VPN VPN di MainActivity DIGANTI membuka ShieldActivity.
- Copy Indonesia existing (`GASKAN` dsb.) dipertahankan di dialog yang tersisa.
- Test unit (junit 4.13.2 sudah ada) ditulis untuk parser murni; CI gate tetap `assembleDebug` (test tidak dijalankan CI — opsional via Android Studio).
- Verbatim spec: `docs/superpowers/specs/2026-09-07-dns-shield-ublock-engine-design.md`.

## File Structure

```
app/src/main/assets/filters/{easylist,easyprivacy,adguard_dns}.txt   (Task 1)
domain/model/DnsShieldModels.kt                                      (Task 2)
data/filter/FilterListParser.kt + test                               (Task 3)
data/vpn/{IpPacket,DnsPacketParser,DnsResponder,DnsForwarder}.kt + test (Task 4)
domain/repository/DnsShieldRepository.kt                             (Task 6)
data/filter/FilterListStore.kt                                       (Task 6)
data/repository/DnsShieldRepositoryImpl.kt                           (Task 6)
di/RepositoryModule.kt (modify)                                      (Task 6)
data/worker/FilterListUpdateWorker.kt + OmnixApplication + manifest  (Task 7)
LocalFirewallService.kt (rewrite) + VpnConfig.kt (defaults)          (Task 8)
presentation/ShieldViewModel.kt                                      (Task 9)
presentation/ShieldActivity.kt + manifest + MainActivity             (Task 10)
```

---

### Task 1: Branch + filter list assets

**Files:**
- Create: `app/src/main/assets/filters/easylist.txt`, `easyprivacy.txt`, `adguard_dns.txt`

**Interfaces:** Produces: assets yang dibaca `FilterListStore` (Task 6) via `context.assets.open("filters/<assetFile>")` — nama file HARUS persis dengan `FilterListId.assetFile` di Task 2.

- [ ] **Step 1: Branch**

```powershell
git checkout -b feat/dns-shield-engine
```

- [ ] **Step 2: Download 3 lists**

```powershell
New-Item -ItemType Directory -Force -Path "app\src\main\assets\filters" | Out-Null
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt" -OutFile "app\src\main\assets\filters\easylist.txt"
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/privacy.txt" -OutFile "app\src\main\assets\filters\easyprivacy.txt"
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/AdguardTeam/AdguardSDNSFilter/master/Filters/AdGuardDNSFilter.txt" -OutFile "app\src\main\assets\filters\adguard_dns.txt"
Get-ChildItem "app\src\main\assets\filters"
```

- [ ] **Step 3: Verifikasi** — 3 file, easylist > 1 MB, easyprivacy > 300 KB, adguard_dns > 300 KB. Jika 404 → BLOCKED (jangan improvizasi URL tanpa mencatat).

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/assets
git commit -m "feat(shield): bundle EasyList, EasyPrivacy, AdGuard DNS filter lists"
```

---

### Task 2: Domain models (DnsShieldModels.kt)

**Files:**
- Create: `app/src/main/java/com/optimizer/android/domain/model/DnsShieldModels.kt`

**Interfaces:**
- Produces (Task 3–10 mengonsumsi persis):
  - `enum class FilterListId(val title: String, val assetFile: String, val updateUrl: String)` — EASYLIST("EasyList","easylist.txt","https://easylist.to/easylist/easylist.txt"), EASYPRIVACY("EasyPrivacy","easyprivacy.txt","https://easylist.to/easylist/easyprivacy.txt"), ADGUARD_DNS("AdGuard DNS","adguard_dns.txt","https://raw.githubusercontent.com/AdguardTeam/AdGuardSDNSFilter/gh-pages/Filters/filter.txt")
  - `data class ShieldStats(total, allowed, blocked, ignored: Long)`
  - `data class BlockedLogEntry(domain: String, listTitle: String, timestamp: Long)`
  - `data class FilterListStatus(id: FilterListId, enabled: Boolean, ruleCount: Int)`
  - `data class ShieldConfig(paused: Boolean, enabledLists: Set<FilterListId>, allowlist: Set<String>)`
  - `sealed class ShieldDecision { object Allow; data class Block(val listTitle: String) }`

- [ ] **Step 1: Tulis file**

```kotlin
package com.optimizer.android.domain.model

enum class FilterListId(val title: String, val assetFile: String, val updateUrl: String) {
    EASYLIST("EasyList", "easylist.txt", "https://easylist.to/easylist/easylist.txt"),
    EASYPRIVACY("EasyPrivacy", "easyprivacy.txt", "https://easylist.to/easylist/easyprivacy.txt"),
    ADGUARD_DNS("AdGuard DNS", "adguard_dns.txt", "https://raw.githubusercontent.com/AdguardTeam/AdGuardSDNSFilter/gh-pages/Filters/filter.txt")
}

data class ShieldStats(
    val total: Long = 0,
    val allowed: Long = 0,
    val blocked: Long = 0,
    val ignored: Long = 0
)

data class BlockedLogEntry(val domain: String, val listTitle: String, val timestamp: Long)

data class FilterListStatus(val id: FilterListId, val enabled: Boolean, val ruleCount: Int)

data class ShieldConfig(
    val paused: Boolean = false,
    val enabledLists: Set<FilterListId> = FilterListId.values().toSet(),
    val allowlist: Set<String> = emptySet()
)

sealed class ShieldDecision {
    object Allow : ShieldDecision()
    data class Block(val listTitle: String) : ShieldDecision()
}
```

- [ ] **Step 2: Commit**

```powershell
git add app/src/main/java/com/optimizer/android/domain/model/DnsShieldModels.kt
git commit -m "feat(shield): add domain models for DNS shield engine"
```

---

### Task 3: FilterListParser (+ unit test)

**Files:**
- Create: `app/src/main/java/com/optimizer/android/data/filter/FilterListParser.kt`
- Test: `app/src/test/java/com/optimizer/android/data/filter/FilterListParserTest.kt`

**Interfaces:**
- Consumes: `FilterListId` (Task 2).
- Produces: `object FilterListParser` dengan `data class Result(blocked: Set<String>, allowed: Set<String>, ruleCount: Int)`, `fun parse(content: String): Result`, `fun checkDomain(sets: Set<String>, domain: String): Boolean` (suffix-match: domain itu sendiri + semua parent-nya).

- [ ] **Step 1: Tulis test dulu (TDD)**

```kotlin
package com.optimizer.android.data.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterListParserTest {

    @Test
    fun parsesAdblockDomainRule() {
        val r = FilterListParser.parse("||ads.example.com^\n")
        assertTrue(r.blocked.contains("ads.example.com"))
        assertEquals(1, r.ruleCount)
    }

    @Test
    fun parsesExceptionRule() {
        val r = FilterListParser.parse("@@||ok.example.com^\n||ads.example.com^\n")
        assertTrue(r.allowed.contains("ok.example.com"))
        assertTrue(r.blocked.contains("ads.example.com"))
 Mahmoud   }

    @Test
    fun parsesHostsRule() {
        val r = FilterListParser.parse("0.0.0.0 tracker.net\n127.0.0.1 spy.io\n")
        assertTrue(r.blocked.contains("tracker.net") && r.blocked.contains("spy.io"))
    }

    @Test
    fun skipsCommentsAndComplexRules() {
        val r = FilterListParser.parse("! comment\n# anchor\n/banner\\d+/\nexample.com##.cls\n||real.domain^\n")
        assertTrue(r.blocked.contains("real.domain"))
        assertEquals(1, r.ruleCount)
    }

    @Test
    fun suffixMatchCoversSubdomains() {
        val sets = setOf("example.com")
        assertTrue(FilterListParser.checkDomain(sets, "example.com"))
        assertTrue(FilterListParser.checkDomain(sets, "ads.example.com"))
        assertTrue(FilterListParser.checkDomain(sets, "a.b.example.com"))
        assertFalse(FilterListParser.checkDomain(sets, "notexample.com"))
        assertFalse(FilterListParser.checkDomain(sets, "example.org"))
    }
}
```

(Perbaiki jika ada typo copy — kode di atas harus compile: `checkDomain` dipanggil sebagai `FilterListParser.checkDomain(...)`.)

- [ ] **Step 2: Tulis implementasi**

```kotlin
package com.optimizer.android.data.filter

object FilterListParser {

    data class Result(val blocked: Set<String>, val allowed: Set<String>, val ruleCount: Int)

    fun parse(content: String): Result {
        val blocked = HashSet<String>()
        val allowed = HashSet<String>()
        var count = 0
        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("!") || line.startsWith("#") || line.startsWith("[")) return@forEach
            val isException = line.startsWith("@@")
            val body = if (isException) line.substring(2) else line

            // Hosts style: 0.0.0.0 domain
            val parts = body.split(Regex("\\s+"))
            if (parts.size == 2 && (parts[0] == "0.0.0.0" || parts[0] == "127.0.0.1")) {
                val d = parts[1].lowercase().trimEnd('.')
                if (d.isNotEmpty()) {
                    if (isException) allowed.add(d) else blocked.add(d)
                    count++
                }
                return@forEach
            }

            // Adblock-style: ||domain^  (exception: @@||domain^)
            if (body.startsWith("||")) {
                var domain = body.substring(2)
                val cut = domain.indexOfFirst { it == '^' || it == '$' || it == '/' || it == '*' }
                if (cut >= 0) domain = domain.substring(0, cut)
                domain = domain.lowercase().trimEnd('.')
                if (domain.isEmpty() || domain.contains("/") || domain.contains(" ")) return@forEach
                if (isException) allowed.add(domain) else blocked.add(domain)
                count++
            }
        }
        return Result(blocked, allowed, count)
    }

    /** Suffix match: domain itu sendiri atau salah satu parent-nya ada di set. */
    fun checkDomain(sets: Set<String>, domain: String): Boolean {
        if (sets.contains(domain)) return true
        var idx = domain.indexOf('.')
        while (idx > 0) {
            if (sets.contains(domain.substring(idx + 1))) return true
            idx = domain.indexOf('.', idx + 1)
        }
        return false
    }
}
```

- [ ] **Step 3: Verifikasi** — checklist: semua 5 test benar secara logika; tidak ada API Android di parser (pure Kotlin — testable JVM).

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/optimizer/android/data/filter app/src/test/java/com/optimizer/android/data/filter
git commit -m "feat(shield): add uAssets filter list parser with suffix matching"
```

---

### Task 4: Byte-level DNS (IpPacket, DnsPacketParser, DnsResponder) + test

**Files:**
- Create: `app/src/main/java/com/optimizer/android/data/vpn/IpPacket.kt`
- Create: `app/src/main/java/com/optimizer/android/data/vpn/DnsPacketParser.kt`
- Create: `app/src/main/java/com/optimizer/android/data/vpn/DnsResponder.kt`
- Test: `app/src/test/java/com/optimizer/android/data/vpn/DnsRoundTripTest.kt`

**Interfaces:**
- Produces (dipakai Task 8):
  - `data class IpUdpPacket(sourceIp: ByteArray, destIp: ByteArray, sourcePort: Int, destPort: Int, payload: ByteArray)`
  - `object IpPacket { fun parseUdp(raw: ByteArray): IpUdpPacket?; fun buildUdp4Response(orig: IpUdpPacket, payload: ByteArray): ByteArray }` — response menukar src/dst IP & port.
  - `data class DnsQuery(id: Int, domain: String, type: Int)`
  - `object DnsPacketParser { fun parse(payload: ByteArray): DnsQuery? }` — null jika bukan query DNS valid.
  - `object DnsResponder { fun blockedResponse(query: DnsQuery): ByteArray; fun servfailResponse(query: DnsQuery): ByteArray; fun nxdomainResponse(query: DnsQuery): ByteArray }` — blocked: A→0.0.0.0, AAAA→::, lainnya NXDOMAIN. (Semua mengembalikan payload DNS saja — pembungkus IP/UDP oleh IpPacket.)

- [ ] **Step 1: Tulis IpPacket.kt**

```kotlin
package com.optimizer.android.data.vpn

data class IpUdpPacket(
    val sourceIp: ByteArray,
    val destIp: ByteArray,
    val sourcePort: Int,
    val destPort: Int,
    val payload: ByteArray
)

object IpPacket {

    /** Parse paket IPv4+UDP mentah dari TUN. Null jika bukan IPv4/UDP/rusak. */
    fun parseUdp(raw: ByteArray): IpUdpPacket? {
        if (raw.size < 28) return null
        if (((raw[0].toInt() shr 4) and 0xF) != 4) return null
        val ihl = (raw[0].toInt() and 0xF) * 4
        if (raw.size < ihl + 8) return null
        if ((raw[9].toInt() and 0xFF) != 17) return null
        val src = raw.copyOfRange(12, 16)
        val dst = raw.copyOfRange(16, 20)
        val sport = ((raw[ihl].toInt() and 0xFF) shl 8) or (raw[ihl + 1].toInt() and 0xFF)
        val dport = ((raw[ihl + 2].toInt() and 0xFF) shl 8) or (raw[ihl + 3].toInt() and 0xFF)
        val payload = raw.copyOfRange(ihl + 8, raw.size)
        return IpUdpPacket(src, dst, sport, dport, payload)
    }

    /** Bangun respons IPv4+UDP dengan src/dst dibalik dari paket asli. */
    fun buildUdp4Response(orig: IpUdpPacket, payload: ByteArray): ByteArray {
        val totalLen = 20 + 8 + payload.size
        val out = ByteArray(totalLen)
        out[0] = 0x45
        out[2] = ((totalLen shr 8) and 0xFF).toByte()
        out[3] = (totalLen and 0xFF).toByte()
        out[6] = 0x40 // DF
        out[8] = 64   // TTL
        out[9] = 17   // UDP
        for (i in 0..3) {
            out[12 + i] = orig.destIp[i]   // new src = orig dst
            out[16 + i] = orig.sourceIp[i] // new dst = orig src
        }
        val u = 20
        out[u] = ((orig.destPort shr 8) and 0xFF).toByte()
        out[u + 1] = (orig.destPort and 0xFF).toByte()
        out[u + 2] = ((orig.sourcePort shr 8) and 0xFF).toByte()
        out[u + 3] = (orig.sourcePort and 0xFF).toByte()
        val udpLen = 8 + payload.size
        out[u + 4] = ((udpLen shr 8) and 0xFF).toByte()
        out[u + 5] = (udpLen and 0xFF).toByte()
        out[u + 6] = 0; out[u + 7] = 0 // UDP checksum 0 valid utk IPv4
        payload.copyInto(out, 28)
        val sum = checksum(out, 0, 20)
        out[10] = ((sum shr 8) and 0xFF).toByte()
        out[11] = (sum and 0xFF).toByte()
        return out
    }

    fun checksum(data: ByteArray, offset: Int, count: Int): Int {
        var sum = 0L
        var i = offset
        val end = offset + count
        while (i < end - 1) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (count % 2 == 1) sum += (data[end - 1].toInt() and 0xFF) shl 8
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv() and 0xFFFF
    }
}
```

- [ ] **Step 2: Tulis DnsPacketParser.kt**

```kotlin
package com.optimizer.android.data.vpn

data class DnsQuery(val id: Int, val domain: String, val type: Int)

object DnsPacketParser {

    /** Payload DNS (tanpa header IP/UDP). Null jika bukan query valid. */
    fun parse(payload: ByteArray): DnsQuery? {
        if (payload.size < 17) return null
        val flags = ((payload[2].toInt() and 0xFF) shl 8) or (payload[3].toInt() and 0xFF)
        if (flags and 0x8000 != 0) return null // QR=1 → response, bukan query
        val qdcount = ((payload[4].toInt() and 0xFF) shl 8) or (payload[5].toInt() and 0xFF)
        if (qdcount < 1) return null
        val id = ((payload[0].toInt() and 0xFF) shl 8) or (payload[1].toInt() and 0xFF)
        var off = 12
        val labels = ArrayList<String>(8)
        var nameLen = 0
        while (true) {
            if (off >= payload.size) return null
            val len = payload[off].toInt() and 0xFF
            if (len == 0) { off += 1; break }
            if (len and 0xC0 != 0) return null // compression pointer di question: tolak
            if (off + 1 + len > payload.size) return null
            nameLen += len + 1
            if (nameLen > 255) return null
            labels.add(String(payload, off + 1, len, Charsets.US_ASCII))
            off += 1 + len
        }
        if (labels.isEmpty()) return null
        if (off + 4 > payload.size) return null
        val type = ((payload[off].toInt() and 0xFF) shl 8) or (payload[off + 1].toInt() and 0xFF)
        return DnsQuery(id, labels.joinToString(".").lowercase(), type)
    }
}
```

- [ ] **Step 3: Tulis DnsResponder.kt**

```kotlin
package com.optimizer.android.data.vpn

object DnsResponder {

    private const val RCODE_NXDOMAIN = 3
    private const val RCODE_SERVFAIL = 2
    private const val TYPE_A = 1
    private const val TYPE_AAAA = 28

    /** A → 0.0.0.0, AAAA → ::, selain itu → NXDOMAIN. */
    fun blockedResponse(query: DnsQuery): ByteArray = when (query.type) {
        TYPE_A -> withAnswer(query, 4, ByteArray(4))
        TYPE_AAAA -> withAnswer(query, 16, ByteArray(16))
        else -> nxdomainResponse(query)
    }

    fun servfailResponse(query: DnsQuery): ByteArray = withRcodeOnly(query, RCODE_SERVFAIL)

    fun nxdomainResponse(query: DnsQuery): ByteArray = withRcodeOnly(query, RCODE_NXDOMAIN)

    fun encodeQuestion(domain: String, type: Int): ByteArray {
        val labels = domain.split('.')
        var size = 1 + labels.sumOf { it.length + 1 } + 4
        val out = ByteArray(size)
        var off = 0
        for (label in labels) {
            val bytes = label.toByteArray(Charsets.US_ASCII)
            out[off] = bytes.size.toByte()
            bytes.copyInto(out, off + 1)
            off += 1 + bytes.size
        }
        out[off] = 0
        off += 1
        out[off] = ((type shr 8) and 0xFF).toByte()
        out[off + 1] = (type and 0xFF).toByte()
        out[off + 2] = 0; out[off + 3] = 1 // IN
        return out
    }

    private fun withAnswer(query: DnsQuery, rdataLen: Int, rdata: ByteArray): ByteArray {
        val q = encodeQuestion(query.domain, query.type)
        val size = 12 + q.size + 12 + rdataLen
        val out = ByteArray(size)
        out[0] = ((query.id shr 8) and 0xFF).toByte()
        out[1] = (query.id and 0xFF).toByte()
        out[2] = 0x81.toByte(); out[3] = 0x80.toByte() // QR=1 RD=1 RA=1 rcode=0
        out[5] = 1 // QDCOUNT=1
        out[7] = 1 // ANCOUNT=1
        encodeQuestion(query.domain, query.type).copyInto(out, 12)
        var off = 12 + q.size
        out[off] = 0xC0.toByte(); out[off + 1] = 0x0C // pointer ke QNAME
        out[off + 2] = ((query.type shr 8) and 0xFF).toByte()
        out[off + 3] = (query.type and 0xFF).toByte()
        out[off + 4] = 0; out[off + 5] = 1 // IN
        out[off + 6] = 0; out[off + 7] = 0; out[off + 8] = 0; out[off + 9] = 60 // TTL 60s
        out[off + 10] = ((rdataLen shr 8) and 0xFF).toByte()
        out[off + 11] = (rdataLen and 0xFF).toByte()
        return out
    }

    private fun withRcodeOnly(query: DnsQuery, rcode: Int): ByteArray {
        val q = encodeQuestion(query.domain, query.type)
        val out = ByteArray(12 + q.size)
        out[0] = ((query.id shr 8) and 0xFF).toByte()
        out[1] = (query.id and 0xFF).toByte()
        out[2] = 0x81.toByte()
        out[3] = (0x80 or rcode).toByte() // QR=1 RD=1 RA=1 + rcode
        out[5] = 1 // QDCOUNT=1, ANCOUNT=0
        q.copyInto(out, 12)
        return out
    }
}
```

(Catatan: `withAnswer` memakai `size` dan mengisi QNAME via `encodeQuestion` dua kali — alokasi kecil, kejelasan > mikro-optimasi di v1.)

- [ ] **Step 4: Tulis test round-trip**

```kotlin
package com.optimizer.android.data.vpn

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DnsRoundTripTest {

    private fun buildQueryBytes(domain: String, type: Int, id: Int = 0x1234): ByteArray {
        val q = DnsResponder.encodeQuestion(domain, type)
        val out = ByteArray(12 + q.size)
        out[0] = ((id shr 8) and 0xFF).toByte(); out[1] = (id and 0xFF).toByte()
        out[5] = 1 // QDCOUNT=1
        q.copyInto(out, 12)
        return out
    }

    @Test
    fun parsesValidQuery() {
        val q = DnsPacketParser.parse(buildQueryBytes("ads.example.com", 1))
        assertEquals("ads.example.com", q?.domain)
        assertEquals(1, q?.type)
        assertEquals(0x1234, q?.id)
    }

    @Test
    fun rejectsGarbageAndResponses() {
        assertNull(DnsPacketParser.parse(byteArrayOf(1, 2, 3)))
        val resp = buildQueryBytes("x.com", 1).clone()
        resp[2] = resp[2].or(0x80.toByte()) // QR=1
        assertNull(DnsPacketParser.parse(resp))
    }

    @Test
    fun blockedResponseAIsZeroIp() {
        val q = DnsQuery(0x1234, "ads.example.com", 1)
        val resp = DnsResponder.blockedResponse(q)
        assertEquals(0x1234, ((resp[0].toInt() and 0xFF) shl 8) or (resp[1].toInt() and 0xFF))
        assertEquals(1, resp[7].toInt()) // ANCOUNT=1
        val rdStart = resp.size - 4
        assertArrayEquals(ByteArray(4), resp.copyOfRange(rdStart(resp), rdStart(resp) + 4))
    }

    private fun rdStart(resp: ByteArray): Int = resp.size - 4

    @Test
    fun ipPacketRoundTrip() {
        val dnsPayload = buildQueryBytes("test.org", 28)
        val orig = ByteArray(28 + dnsPayload.size)
        orig[0] = 0x45
        orig[9] = 17
        for (i in 0..3) { orig[12 + i] = 10; orig[16 + i] = 20 } // src=10.x dst=20.x
        orig[20] = 0x30; orig[21] = 0x39 // sport 12345
        orig[22] = 0; orig[23] = 53      // dport 53
        dnsPayload.copyInto(orig, 28)
        val pkt = IpPacket.parseUdp(orig)
        assertEquals(12345, pkt?.sourcePort)
        assertEquals(53, pkt?.destPort)
        val resp = IpPacket.buildUdp4Response(pkt!!, DnsResponder.blockedResponse(DnsQuery(1, "test.org", 28)))
        // src/dst tertukar: dst byte ke-12..15 harus 10.x (src asli)
        assertEquals(20, resp[16].toInt() and 0xFF)
        assertNull(IpPacket.parseUdp(byteArrayOf(0, 1, 2)))
    }
}
```

(Jika ada kesalahan sintaks kecil pada test saat implementasi, perbaiki test — assert maksudnya jelas: ID ter-copy, ANCOUNT=1, 4 byte terakhir = 0.0.0.0.)

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/optimizer/android/data/vpn app/src/test/java/com/optimizer/android/data/vpn
git commit -m "feat(shield): add byte-level DNS packet parser, responder, IP packet builder"
```

---

### Task 5: DnsForwarder

**Files:**
- Create: `app/src/main/java/com/optimizer/android/data/vpn/DnsForwarder.kt`

**Interfaces:**
- Produces: `class DnsForwarder(private val vpnService: VpnService)` dengan `fun forward(payload: ByteArray): ByteArray?` — kirim payload DNS ke upstream via DatagramSocket yang di-protect, timeout 5s; null jika gagal.

- [ ] **Step 1: Tulis file**

```kotlin
package com.optimizer.android.data.vpn

import android.net.VpnService
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DnsForwarder(private val vpnService: VpnService) {

    companion object {
        private const val UPSTREAM = "94.140.14.14"
        private const val UPSTREAM_PORT = 53
        private const val TIMEOUT_MS = 5000
    }

    /** Kirim query ke upstream AdGuard, balikkan payload jawaban. Null = gagal/timeout. */
    fun forward(payload: ByteArray): ByteArray? {
        val socket = DatagramSocket()
        return try {
            vpnService.protect(socket) // wajib — tanpa ini socket masuk TUN lagi
            socket.soTimeout = TIMEOUT_MS
            val addr = InetAddress.getByName(UPSTREAM)
            socket.send(DatagramPacket(payload, payload.size, addr, UPSTREAM_PORT))
            val buf = ByteArray(4096)
            val recv = DatagramPacket(buf, buf.size)
            socket.receive(recv)
            recv.data.copyOf(recv.length)
        } catch (e: Exception) {
            null
        } finally {
            socket.close()
        }
    }
}
```

- [ ] **Step 2: Commit**

```powershell
git add app/src/main/java/com/optimizer/android/data/vpn/DnsForwarder.kt
git commit -m "feat(shield): add protected-socket DNS forwarder to AdGuard upstream"
```

---

### Task 6: Repository + Store + DI

**Files:**
- Create: `app/src/main/java/com/optimizer/android/domain/repository/DnsShieldRepository.kt`
- Create: `app/src/main/java/com/optimizer/android/data/filter/FilterListStore.kt`
- Create: `app/src/main/java/com/optimizer/android/data/repository/DnsShieldRepositoryImpl.kt`
- Modify: `app/src/main/java/com/optimizer/android/di/RepositoryModule.kt`

**Interfaces:**
- Consumes: `FilterListId/ShieldStats/BlockedLogEntry/FilterListStatus/ShieldConfig/ShieldDecision` (Task 2), `FilterListParser` (Task 3), `FilterListStore` (dibuat di task ini).
- Produces:
  - `class FilterListStore @Inject constructor(@ApplicationContext context: Context)` — `readBundled(id): String`, `readUpdated(id): String?` (file filesDir > 10KB), `writeUpdated(id, content): Boolean`
  - `interface DnsShieldRepository` — lihat kode; `checkDomain(domain): ShieldDecision` adalah fungsi utama engine.

- [ ] **Step 1: Tulis FilterListStore.kt**

```kotlin
package com.optimizer.android.data.filter

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterListStore @Inject constructor(@ApplicationContext private val context: Context) {

    fun readBundled(id: com.optimizer.android.domain.model.FilterListId): String = try {
        context.assets.open("filters/${id.assetFile}").bufferedReader().use { it.readText() }
    } catch (e: Exception) { "" }

    fun readUpdated(id: com.optimizer.android.domain.model.FilterListId): String? = try {
        val f = File(context.filesDir, "filters/${id.assetFile}")
        if (f.exists() && f.length() > 10_240) f.readText() else null
    } catch (e: Exception) { null }

    fun writeUpdated(id: com.optimizer.android.domain.model.FilterListId, content: String): Boolean = try {
        val dir = File(context.filesDir, "filters")
        if (!dir.exists()) dir.mkdirs()
        File(dir, id.assetFile).writeText(content)
        true
    } catch (e: Exception) { false }
}
```

(Boleh di-import biasa alih-alih fully-qualified `FilterListId` — idiomatik.)

- [ ] **Step 2: Tulis DnsShieldRepository.kt (interface)**

```kotlin
package com.optimizer.android.domain.repository

import com.optimizer.android.domain.model.BlockedLogEntry
import com.optimizer.android.domain.model.FilterListId
import com.optimizer.android.domain.model.FilterListStatus
import com.optimizer.android.domain.model.ShieldDecision
import com.optimizer.android.domain.model.ShieldStats
import kotlinx.coroutines.flow.StateFlow

interface DnsShieldRepository {
    val stats: StateFlow<ShieldStats>
    val blockedLog: StateFlow<List<BlockedLogEntry>>
    val listStatuses: StateFlow<List<FilterListStatus>>
    val paused: StateFlow<Boolean>
    val lastUpdate: StateFlow<Long?>
    val engineVersion: StateFlow<Int>

    /** Keputusan untuk satu domain. Dipanggil engine loop per query. */
    fun checkDomain(domain: String): ShieldDecision
    fun incrementTotal()
    fun incrementIgnored()
    fun incrementAllowed()
    fun recordBlocked(domain: String, listTitle: String)
    fun allowlistSnapshot(): List<String>
    fun reloadLists()
    suspend fun updateLists(): Boolean
    fun setPaused(value: Boolean)
    fun setListEnabled(id: FilterListId, enabled: Boolean)
    fun addAllowDomain(domain: String)
    fun removeAllowDomain(domain: String)
}
```

- [ ] **Step 3: Tulis DnsShieldRepositoryImpl.kt**

```kotlin
package com.optimizer.android.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.optimizer.android.data.filter.FilterListParser
import com.optimizer.android.data.filter.FilterListStore
import com.optimizer.android.domain.model.BlockedLogEntry
import com.optimizer.android.domain.model.FilterListId
import com.optimizer.android.domain.model.FilterListStatus
import com.optimizer.android.domain.model.ShieldDecision
import com.optimizer.android.domain.model.ShieldStats
import com.optimizer.android.domain.repository.DnsShieldRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsShieldRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: android.content.Context
) : DnsShieldRepository {

    private val store = FilterListStore(context)
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dns_shield", android.content.Context.MODE_PRIVATE)

    private val blockedByList = ConcurrentHashMap<FilterListId, Set<String>>()
    private val allowedByList = ConcurrentHashMap<FilterListId, Set<String>>()
    private val userAllow: MutableSet<String> = ConcurrentHashMap.newKeySet<String>().also { s ->
        prefs.getStringSet(KEY_ALLOW, emptySet())?.forEach { s.add(it.lowercase()) }
    }

    private val _stats = kotlinx.coroutines.flow.MutableStateFlow(ShieldStats())
    override val stats: StateFlow<ShieldStats> = _stats.asStateFlow()
    private val _blockedLog = kotlinx.coroutines.flow.MutableStateFlow<List<BlockedLogEntry>>(emptyList())
    override val blockedLog: StateFlow<List<BlockedLogEntry>> = _blockedLog.asStateFlow()
    private val _listStatuses = kotlinx.coroutines.flow.MutableStateFlow<List<FilterListStatus>>(emptyList())
    override val listStatuses: StateFlow<List<FilterListStatus>> = _listStatuses.asStateFlow()
    private val _paused = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean(KEY_PAUSED, false))
    override val paused: StateFlow<Boolean> = _paused.asStateFlow()
    private val _lastUpdate = kotlinx.coroutines.flow.MutableStateFlow<Long?>(prefs.getLong(KEY_LAST_UPDATE, 0L).takeIf { it > 0 })
    override val lastUpdate: StateFlow<Long?> = _lastUpdate.asStateFlow()
    private val _engineVersion = kotlinx.coroutines.flow.MutableStateFlow(0)
    override val engineVersion: StateFlow<Int> = _engineVersion.asStateFlow()

    private val listEnabled = ConcurrentHashMap<FilterListId, Boolean>().apply {
        val saved = prefs.getStringSet(KEY_ENABLED, FilterListId.values().map { it.name }.toSet())
        FilterListId.values().forEach { put(it, saved?.contains(it.name) != false) }
    }

    init { reloadLists() }

    override fun checkDomain(domain: String): ShieldDecision {
        if (FilterListParser.checkDomain(userAllowSet(), domain)) return ShieldDecision.Allow
        FilterListId.values().forEach { id ->
            if (listEnabled[id] == true) {
                val allow = allowedByList[id]
                if (allow != null && FilterListParser.checkDomain(allow, domain)) return ShieldDecision.Allow
            }
        }
        FilterListId.values().forEach { id ->
            if (listEnabled[id] == true) {
                val block = blockedByList[id]
                if (block != null && FilterListParser.checkDomain(block, domain)) return ShieldDecision.Block(id.title)
            }
        }
        return ShieldDecision.Allow
    }

    override fun incrementTotal() { _stats.update { it.copy(total = it.total + 1) } }
    override fun incrementIgnored() { _stats.update { it.copy(ignored = it.ignored + 1) } }

    fun incrementAllowed() { _stats.update { it.copy(allowed = it.allowed + 1) } }

    override fun recordBlocked(domain: String, listTitle: String) {
        _stats.update { it.copy(blocked = it.blocked + 1) }
        _blockedLog.update { current ->
            (BlockedLogEntry(domain, listTitle, System.currentTimeMillis()) + current).take(200)
        }
    }

    override fun reloadLists() {
        val statuses = ArrayList<FilterListStatus>()
        FilterListId.values().forEach { id ->
            if (listEnabled[id] != true) { statuses.add(FilterListStatus(id, false, 0)); return@forEach }
            val content = store.readUpdated(id).takeIf { !it.isNullOrBlank() } ?: store.readBundled(id)
            val result = FilterListParser.parse(content)
            if (result.ruleCount < 100) {
                // korup/terlalu kecil → buang, list ini kosong (fallback berikutnya via update)
                blockedByList[id] = emptySet(); allowedByList[id] = emptySet()
                statuses.add(FilterListStatus(id, true, 0))
            } else {
                blockedByList[id] = result.blocked
                allowedByList[id] = result.allowed
                statuses.add(FilterListStatus(id, true, result.ruleCount))
            }
        }
        _listStatuses.value = statuses
        _engineVersion.update { it + 1 }
    }

    override suspend fun updateLists(): Boolean = withContext(Dispatchers.IO) {
        var anySuccess = false
        FilterListId.values().forEach { id ->
            if (listEnabled[id] != true) return@forEach
            val content = try {
                val conn = URL(id.updateUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 30_000
                conn.readTimeout = 30_000
                conn.setRequestProperty("User-Agent", "OMNIX-OS/1.0")
                conn.inputStream.bufferedReader().use { it.readText() }
            } catch (e: Exception) { null }
            if (content != null && content.length > 10_240) {
                val parsed = FilterListParser.parse(content)
                if (parsed.ruleCount >= 100 && store.writeUpdated(id, content)) anySuccess = true
            }
        }
        if (anySuccess) {
            reloadLists()
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LAST_UPDATE, now).apply()
            _lastUpdate.value = now
        }
        anySuccess
    }

    override fun setPaused(value: Boolean) {
        _paused.value = value
        prefs.edit().putBoolean(KEY_PAUSED, value).apply()
    }

    override fun setListEnabled(id: FilterListId, enabled: Boolean) {
        listEnabled[id] = enabled
        prefs.edit().putStringSet(KEY_ENABLED, listEnabled.filterValues { it }.keys.toSet()).apply()
        reloadLists()
    }

    override fun addAllowDomain(domain: String) {
        val d = domain.lowercase().trim().trimEnd('.')
        if (d.isEmpty()) return
        userAllow.add(d)
        prefs.edit().putStringSet(KEY_ALLOW, userAllow.toSet()).apply()
    }

    override fun removeAllowDomain(domain: String) {
        userAllow.remove(domain.lowercase())
        prefs.edit().putStringSet(KEY_ALLOW, userAllow.toSet()).apply()
    }

    private fun userAllowSet(): Set<String> = userAllow

    override fun allowlistSnapshot(): List<String> = userAllow.toList().sorted()

    companion object {
        private const val KEY_PAUSED = "paused"
        private const val KEY_ENABLED = "enabled_lists"
        private const val KEY_ALLOW = "allowlist"
        private const val KEY_LAST_UPDATE = "last_update"
    }
}
```

(Saat implementasi: ganti fully-qualified `kotlinx.coroutines.flow.MutableStateFlow` dengan import biasa + `asStateFlow`; `userAllow` bertipe `MutableSet<String> = ConcurrentHashMap.newKeySet()`; seluruh method interface bertanda `override`.)

- [ ] **Step 4: Daftarkan DI di RepositoryModule.kt** (tambah binding, ikuti pola existing):

```kotlin
@Binds
@Singleton
fun bindDnsShieldRepository(dnsShieldRepositoryImpl: DnsShieldRepositoryImpl): DnsShieldRepository
```

(+ import `com.optimizer.android.domain.repository.DnsShieldRepository` dan `com.optimizer.android.data.repository.DnsShieldRepositoryImpl`.)

- [ ] **Step 5: Verifikasi** — interface impl match 1:1; `incrementAllowed`/`recordBlocked` dipanggil engine Task 8; DI bind ada.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/optimizer/android
git commit -m "feat(shield): add DnsShieldRepository with allowlist, config persist, list loader"
```

---

### Task 7: FilterListUpdateWorker + INTERNET permission + scheduling

**Files:**
- Create: `app/src/main/java/com/optimizer/android/data/worker/FilterListUpdateWorker.kt`
- Modify: `app/src/main/AndroidManifest.xml` (tambah `<uses-permission android:name="android.permission.INTERNET" />` di atas `<application>`)
- Modify: `app/src/main/java/com/optimizer/android/OmnixApplication.kt` (schedule worker di onCreate — baca file dulu, ikuti pola existing)

**Interfaces:**
- Consumes: `DnsShieldRepository` (Task 5).
- Produces: `FilterListUpdateWorker.schedule(context: Context)` (unique periodic, 7 hari, KEEP).

- [ ] **Step 1: Tulis FilterListUpdateWorker.kt**

```kotlin
package com.optimizer.android.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.optimizer.android.domain.repository.DnsShieldRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class FilterListUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dnsShieldRepository: DnsShieldRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return if (dnsShieldRepository.updateLists()) Result.success() else Result.retry()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FilterListUpdateWorker>(7, java.util.concurrent.TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "filter_list_update", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
```

- [ ] **Step 2: Manifest + schedule** — tambahkan `<uses-permission android:name="android.permission.INTERNET" />` dan panggil `FilterListUpdateWorker.schedule(this)` di `OmnixApplication.onCreate()` (baca file existing dulu).

- [ ] **Step 3: Commit**

```powershell
git add app/src/main
git commit -m "feat(shield): add 7-day filter list update worker + INTERNET permission"
```

---

### Task 8: LocalFirewallService rewrite + VpnConfig defaults

**Files:**
- Modify: `app/src/main/java/com/optimizer/android/domain/model/VpnConfig.kt` (defaults)
- Modify: `app/src/main/java/com/optimizer/android/LocalFirewallService.kt` (rewrite)

**Interfaces:**
- Consumes: `DnsShieldRepository` (checkDomain/increment*/recordBlocked/paused), `IpPacket/IpUdpPacket`, `DnsPacketParser`, `DnsResponder`, `DnsForwarder`, `ShieldDecision`.
- Produces: `companion object { val running: kotlinx.coroutines.flow.MutableStateFlow<Boolean> }` — dipakai ShieldActivity (Task 10) untuk status dot.

- [ ] **Step 1: Update VpnConfig defaults**

```kotlin
package com.optimizer.android.domain.model

data class VpnConfig(
    val sessionName: String = "DNS Web Shield",
    val localAddress: String = "10.111.222.1",
    val localPrefixLength: Int = 24,
    val dnsServers: List<String> = listOf("10.111.222.3"),
    val routes: List<String> = listOf(
        "10.111.222.3",
        "8.8.8.8", "8.8.4.4", "1.1.1.1", "1.0.0.1", "9.9.9.9",
        "149.112.112.112", "208.67.222.222", "208.67.220.220",
        "94.140.14.14", "94.140.15.15"
    ),
    val routePrefixLength: Int = 32
)
```

- [ ] **Step 2: Rewrite LocalFirewallService.kt**

```kotlin
package com.optimizer.android

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.optimizer.android.data.vpn.DnsForwarder
import com.optimizer.android.data.vpn.DnsPacketParser
import com.optimizer.android.data.vpn.DnsResponder
import com.optimizer.android.data.vpn.IpPacket
import com.optimizer.android.domain.model.ShieldDecision
import com.optimizer.android.domain.repository.DnsShieldRepository
import com.optimizer.android.domain.repository.VpnConfigRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class LocalFirewallService : VpnService() {

    @Inject lateinit var vpnConfigRepository: VpnConfigRepository
    @Inject lateinit var dnsShieldRepository: com.optimizer.android.domain.repository.DnsShieldRepository

    private var vpnInterface: ParcelFileDescriptor? = null
    private var engineJob: Job? = null
    private var tunInput: FileInputStream? = null
    private var tunOutput: FileOutputStream? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val forwarder by lazy { DnsForwarder(this) }

    companion object {
        private const val ACTION_START = "com.optimizer.android.ACTION_START"
        private const val ACTION_STOP = "com.optimizer.android.ACTION_STOP"
        private const val TAG = "LocalFirewallService"
        val running = MutableStateFlow(false)

        fun startIntent(context: Context): Intent = Intent(context, LocalFirewallService::class.java).apply { action = ACTION_START }
        fun stopIntent(context: Context): Intent = Intent(context, LocalFirewallService::class.java).apply { action = ACTION_STOP }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { closeVpnInterface(); stopSelf(); return START_NOT_STICKY }
            ACTION_START -> { startVPN(); return START_STICKY }
        }
        return START_NOT_STICKY
    }

    private fun startVPN() {
        if (vpnInterface == null) {
            scope.launch {
                val vpnConfig = vpnConfigRepository.getVpnConfig()
                val builder = Builder()
                    .setSession(vpnConfig.sessionName)
                    .addAddress(vpnConfig.localAddress, vpnConfig.localPrefixLength)
                vpnConfig.dnsServers.forEach { builder.addDnsServer(it) }
                vpnConfig.routes.forEach { builder.addRoute(it, vpnConfig.routePrefixLength) }
                try {
                    vpnInterface = builder.establish()
                    if (vpnInterface == null) {
                        Log.e(TAG, "Failed to establish VPN: permission denied or not prepared")
                        stopSelf()
                    } else {
                        Log.i(TAG, "VPN established — DNS Shield engine online")
                        runEngine()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to establish VPN interface", e)
                    stopSelf()
                }
            }
        }
    }

    private fun runEngine() {
        val tun = vpnInterface ?: return
        val input = FileInputStream(tun.fileDescriptor)
        val output = FileOutputStream(tun.fileDescriptor)
        tunInput = input; tunOutput = output
        running.value = true
        engineJob = ioScope.launch {
            val buffer = ByteArray(32768)
            while (isActive) {
                try {
                    val n = input.read(buffer)
                    if (n <= 0) break
                    processPacket(buffer, n, output)
                } catch (e: Exception) {
                    Log.w(TAG, "Engine read error", e)
                    break
                }
            }
            running.value = false
        }
    }

    private fun processPacket(buffer: ByteArray, n: Int, output: FileOutputStream) {
        val raw = buffer.copyOf(n)
        val pkt = IpPacket.parseUdp(raw)
        if (pkt == null || pkt.destPort != 53) {
            dnsShieldRepository.incrementIgnored()
            return
        }
        val query = DnsPacketParser.parse(pkt.payload)
        if (query == null) {
            dnsShieldRepository.incrementIgnored()
            return
        }
        dnsShieldRepository.incrementTotal()
        val decision = if (dnsShieldRepository.paused.value) ShieldDecision.Allow
                       else dnsShieldRepository.checkDomain(query.domain)
        when (decision) {
            is ShieldDecision.Block -> {
                val dnsResp = DnsResponder.blockedResponse(query)
                output.write(IpPacket.buildUdp4Response(pkt, dnsResp))
                dnsShieldRepository.recordBlocked(query.domain, decision.listTitle)
            }
            ShieldDecision.Allow -> {
                val upstream = forwarder.forward(pkt.payload)
                val dnsPayload = upstream ?: DnsResponder.servfailResponse(query)
                output.write(IpPacket.buildUdp4Response(pkt, dnsPayload))
                dnsShieldRepository.incrementAllowed()
            }
        }
    }

    private fun closeVpnInterface() {
        try {
            engineJob?.cancel(); engineJob = null
            tunInput?.close(); tunInput = null
            tunOutput?.close(); tunOutput = null
            vpnInterface?.close(); vpnInterface = null
            running.value = false
            Log.i(TAG, "VPN closed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }
    }

    override fun onDestroy() {
        closeVpnInterface()
        scope.cancel(); ioScope.cancel()
        super.onDestroy()
    }
}
```

- [ ] **Step 3: Verifikasi** — `protect(socket)` hanya di DnsForwarder; engine loop cancel di onDestroy; `running` StateFlow companion.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/optimizer/android
git commit -m "feat(shield): rewrite LocalFirewallService as narrow-route DNS sinkhole engine"
```

---

### Task 9: ShieldViewModel

**Files:**
- Create: `app/src/main/java/com/optimizer/android/presentation/ShieldViewModel.kt`

**Interfaces:**
- Consumes: `DnsShieldRepository`, `LocalFirewallService.running` (Task 6).
- Produces: `@HiltViewModel class ShieldViewModel @Inject constructor(...) : ViewModel()` dengan val `stats/blockedLog/listStatuses/paused/lastUpdate/vpnRunning: StateFlow<...>` dan fun `updateLists() / setPaused(v) / setListEnabled(id, v) / addAllow(d) / removeAllow(d) / refreshLists()`.

- [ ] **Step 1: Tulis file**

```kotlin
package com.optimizer.android.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optimizer.android.LocalFirewallService
import com.optimizer.android.domain.model.FilterListId
import com.optimizer.android.domain.repository.DnsShieldRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShieldViewModel @Inject constructor(
    private val dnsShieldRepository: DnsShieldRepository
) : ViewModel() {

    val stats = dnsShieldRepository.stats
    val blockedLog = dnsShieldRepository.blockedLog
    val listStatuses = dnsShieldRepository.listStatuses
    val paused = dnsShieldRepository.paused
    val lastUpdate = dnsShieldRepository.lastUpdate
    val vpnRunning = LocalFirewallService.running

    fun refreshLists() = dnsShieldRepository.reloadLists()

    fun updateLists() {
        viewModelScope.launch { dnsShieldRepository.updateLists() }
    }

    fun setPaused(value: Boolean) = dnsShieldRepository.setPaused(value)

    fun setListEnabled(id: FilterListId, value: Boolean) = dnsShieldRepository.setListEnabled(id, value)

    fun addAllow(domain: String) = dnsShieldRepository.addAllowDomain(domain)

    fun removeAllow(domain: String) = dnsShieldRepository.removeAllowDomain(domain)
}
```

- [ ] **Step 2: Commit**

```powershell
git add app/src/main/java/com/optimizer/android/presentation/ShieldViewModel.kt
git commit -m "feat(shield): add ShieldViewModel exposing engine state to dashboard"
```

---

### Task 10: ShieldActivity + manifest + MainActivity entry

**Files:**
- Create: `app/src/main/java/com/optimizer/android/presentation/ShieldActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml` (activity baru)
- Modify: `app/src/main/java/com/optimizer/android/MainActivity.kt` (kartu WEB SHIELD buka ShieldActivity; hapus blok dialog VPN)

**Interfaces:**
- Consumes: `StatusCard`, `SectionHeader`, `TerminalLog`, `OmnixTheme`, `OmnixThemeColors`, `OmnixType`, `FeatureType.SHIELD` (semua sudah ada); `ShieldViewModel` (Task 9); `LocalFirewallService.startIntent/stopIntent` + `LocalFirewallService.running` (Task 8).
- Produces: ShieldActivity terdaftar di manifest; entry dari kartu WEB SHIELD.

- [ ] **Step 1: Tulis ShieldActivity.kt**

```kotlin
package com.optimizer.android.presentation

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.optimizer.android.LocalFirewallService
import com.optimizer.android.ui.components.SectionHeader
import com.optimizer.android.ui.components.StatusCard
import com.optimizer.android.ui.components.TerminalLog
import com.optimizer.android.ui.theme.FeatureType
import com.optimizer.android.ui.theme.OmnixTheme
import com.optimizer.android.ui.theme.OmnixThemeColors
import com.optimizer.android.ui.theme.OmnixType
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ShieldActivity : ComponentActivity() {

    private val viewModel: ShieldViewModel by viewModels()

    private val vpnLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startService(LocalFirewallService.startIntent(this))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OmnixTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ShieldDashboard()
                }
            }
        }
    }

    @Composable
    private fun ShieldDashboard() {
        val colors = OmnixThemeColors.colors
        val accent = colors.accent(FeatureType.SHIELD)
        val stats by viewModel.stats.collectAsState()
        val blockedLog by viewModel.blockedLog.collectAsState()
        val statuses by viewModel.listStatuses.collectAsState()
        val paused by viewModel.paused.collectAsState()
        val lastUpdate by viewModel.lastUpdate.collectAsState()
        val running by viewModel.vpnRunning.collectAsState()
        var allowInput by remember { mutableStateOf("") }
        var allowTick by remember { mutableIntStateOf(0) }
        val allowList = remember(allowTick) { viewModel.allowlistSnapshot() }
        val dateFormat = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }

        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("WEB SHIELD", style = OmnixType.display, color = colors.ink)
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(12.dp)
                        .background(
                            when {
                                running && !paused -> accent
                                running -> colors.grid
                                else -> colors.grid.copy(alpha = 0.3f)
                            },
                            CircleShape
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        running && !paused -> "AKTIF"
                        running -> "PAUSED"
                        else -> "OFF"
                    },
                    style = OmnixType.label, color = colors.grid
                )
            }

            SectionHeader("01", "STATS")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusCard(Modifier.weight(1f), "TOTAL", "${stats.total}")
                StatusCard(Modifier.weight(1f), "ALLOWED", "${stats.allowed}")
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusCard(Modifier.weight(1f), "BLOCKED", "${stats.blocked}")
                StatusCard(Modifier.weight(1f), "IGNORED", "${stats.ignored}")
            }

            SectionHeader("02", "BLOCKED LOG")
            TerminalLog(logs = blockedLog.map { "${it.domain} [${it.listTitle}]" })

            SectionHeader("03", "FILTER LISTS")
            statuses.forEach { st ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(st.id.title, style = OmnixType.title, color = colors.ink)
                        Text("${st.ruleCount} rules", style = OmnixType.label, color = colors.grid)
                    }
                    Switch(checked = st.enabled, onCheckedChange = { viewModel.setListEnabled(st.id, it) })
                }
            }
            Text(
                "Update terakhir: " + (lastUpdate?.let { dateFormat.format(Date(it)) } ?: "bundel bawaan"),
                style = OmnixType.label, color = colors.grid
            )
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = { viewModel.updateLists() },
                shape = RectangleShape,
                border = BorderStroke(2.dp, accent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
            ) { Text("UPDATE LISTS SEKARANG", style = OmnixType.label) }

            SectionHeader("04", "ALLOWLIST")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = allowInput,
                    onValueChange = { allowInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("domain.com", style = OmnixType.mono, color = colors.grid) },
                    textStyle = OmnixType.mono.copy(color = colors.ink),
                    singleLine = true
                )
                Button(
                    onClick = {
                        viewModel.addAllow(allowInput)
                        allowInput = ""
                        allowTick++
                    },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = colors.base)
                ) { Text("ADD") }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.height(120.dp)) {
                items(allowList, key = { it }) { domain ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(domain, style = OmnixType.mono, color = colors.ink, modifier = Modifier.weight(1f))
                        Text(
                            "HAPUS",
                            style = OmnixType.label,
                            color = colors.danger,
                            modifier = Modifier.clickable {
                                viewModel.removeAllow(domain)
                                allowTick++
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.setPaused(!paused) },
                    enabled = running,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = colors.base)
                ) { Text(if (paused) "RESUME" else "PAUSE") }
                OutlinedButton(
                    onClick = {
                        if (running) {
                            startService(LocalFirewallService.stopIntent(this@ShieldActivity))
                        } else {
                            val intent = VpnService.prepare(this@ShieldActivity)
                            if (intent != null) vpnLauncher.launch(intent)
                            else startService(LocalFirewallService.startIntent(this@ShieldActivity))
                        }
                    },
                    shape = RectangleShape,
                    border = BorderStroke(2.dp, colors.ink),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.ink)
                ) { Text(if (running) "STOP SHIELD" else "START SHIELD") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
```

- [ ] **Step 2: Manifest** � tambahkan setelah AppEraserActivity:

```xml
<activity android:name=".presentation.ShieldActivity" android:exported="false" />
```

- [ ] **Step 3: MainActivity** � kartu "DNS-LEVEL WEB SHIELD": ganti `onClick = { viewModel.toggleDialog(DialogType.VPN) }` menjadi `onClick = { startActivity(Intent(this@MainActivity, ShieldActivity::class.java)) }` (+ import `com.optimizer.android.presentation.ShieldActivity`). Hapus blok dialog `if (uiState.activeDialog == DialogType.VPN) {...}` dan val `shieldAccent` yang kini tak terpakai. `DialogType` enum, `requestVpn()`, `vpnLauncher` DIPERTAHANKAN di MainActivity (tidak merusak apa pun). Verifikasi: grep `DialogType.VPN` di MainActivity = 0 hasil.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main
git commit -m "feat(shield): add uBlock-style ShieldActivity dashboard + wire entry card"
```

---
### Task 11: Push, CI, merge

- [ ] **Step 1: Push** — `git push -u origin feat/dns-shield-engine`
- [ ] **Step 2: CI** — trigger `android-build.yml` di ref branch; tunggu success; jika merah: baca log job, dispatch fix, push ulang, ulangi sampai hijau (iteration policy: JANGAN berhenti saat ada error).
- [ ] **Step 3: Merge** — `git checkout main && git merge --no-ff feat/dns-shield-engine && git push origin main`
- [ ] **Step 4: CI main hijau + artifact APK ada.**
- [ ] **Step 5: QA device (laporkan ke user):** browsing normal lancar; `doubleclick.net` terblokir + counter naik; pause/resume instan; allowlist meng-allow domain; switch list bekerja; update worker menyimpan file.

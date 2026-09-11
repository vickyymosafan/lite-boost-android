# DNS Web Shield v2 — Local uBlock-style Filtering Engine (Design)

Tanggal: 2026-09-11
Status: Disetujui (brainstorming selesai, menunggu implementasi plan)
Predecessor: "DNS-Level Web Shield" existing (LocalFirewallService + VpnConfig)

## 1. Tujuan & Latar

Kondisi existing: LocalFirewallService hanya me-route trafik DNS ke server AdGuard (94.140.14.14) — semua keputusan blocking terjadi remote, tanpa statistik, tanpa kontrol.

Tujuan: engine filtering **lokal ala uBlock Origin** — app sendiri yang memutuskan block/allow per query DNS, dengan filter lists resmi dari repo uBlockOrigin/uAssets (EasyList + EasyPrivacy) + AdGuard DNS filter, plus dashboard statistik real-time.

**Keputusan bersama user:**
- Bentuk "uBlock": **uAssets lists → engine lokal** (bukan GeckoView/ekstensi asli — APK bengkak 100MB+ melawan identitas "super ringan").
- UI: **Dashboard ala uBlock** (stats counters hidup, blocked log, pause/resume, allowlist, switch per list).
- Lists: **Bundel di APK (fallback offline) + auto-update WorkManager 7 hari**.
- Pendekatan arsitektur: **A — Narrow-route DNS Sinkhole** (TUN hanya menangkap DNS, bukan full-tunnel).

**Batasan yang diakui (dari docs AdGuard, diterima user):** filtering DNS-level hanya bisa blokir **level domain** — tidak setara filtering HTTP per-request uBlock (itu butuh MITM). Kualitas blocking setara uBlock pada level domain; rule URL-pattern/cosmetic EasyList diabaikan parser.

## 2. Arsitektur

File baru/berubah (pola existing: domain/data/di/presentation):

```
domain/
  model/DnsShieldModels.kt          // FilterListId, ShieldStats, BlockedLogEntry, ShieldConfig
  repository/DnsShieldRepository.kt // interface: lists, stats flow, allowlist, config
data/
  filter/FilterListParser.kt        // uAssets format → (blockedSet, allowSet)
  filter/FilterListStore.kt         // bundel assets + file hasil update di filesDir
  repository/DnsShieldRepositoryImpl.kt
  vpn/DnsPacketParser.kt            // parse raw UDP DNS dari TUN
  vpn/DnsResponder.kt               // rakit jawaban 0.0.0.0 / ::
  vpn/DnsForwarder.kt               // relay ke upstream AdGuard via protected socket
  worker/FilterListUpdateWorker.kt  // WorkManager, 7 hari
LocalFirewallService.kt             // REWRITE: narrow-route TUN + engine loop
presentation/ShieldViewModel.kt + ShieldActivity.kt  // dashboard ala uBlock
```

Komponen app lain TIDAK diubah (cleaner, camera, studio, theme layer).

## 3. Filter Lists & Parser

**Lists (bundel di `assets/filters/`):**
1. EasyList (uBlockOrigin/uAssets) — iklan
2. EasyPrivacy (uBlockOrigin/uAssets) — pelacak
3. AdGuard DNS filter (AdguardTeam/AdguardSDNSFilter) — khusus domain, paling cocok DNS-level

**Parser mengenali:**
- `||domain^` → blocked
- `@@||domain^` → allowlist/exception
- `0.0.0.0 domain` (hosts-style) → blocked
- `!` / `#` → komentar, skip
- Rule URL-pattern / modifier kompleks → diabaikan (karakteristik DNS-level)

Output: `HashSet<String>` blocked + allow (domain tersimpan tanpa trailing dot). Lookup O(1).

## 4. VPN Narrow-Route (perubahan VpnConfig default)

- localAddress: `10.111.222.1` (prefix 24)
- addDnsServer: `10.111.222.3` (virtual, milik engine)
- routes: /32 ke resolver populer (8.8.8.8, 8.8.4.4, 1.1.1.1, 9.9.9.9, dst.) **+ 10.111.222.3 sendiri** (wajib — tanpa ini, query ke DNS virtual tidak masuk TUN)

Efek: semua app mengarahkan DNS ke engine; trafik non-DNS keluar tunnel → hemat baterai/CPU.

## 5. Data Flow & Blocking Loop

```
establish() TUN
  └─ engine loop (Dispatchers.IO): read datagram (FileInputStream, 32KB buffer)
       → DnsPacketParser.parse(datagram)
           ├─ bukan DNS / rusak → drop + counter ignored
           └─ query valid (header 12B, ID, QNAME, QTYPE):
               ├─ allowlist suffix-match → forward + counter allowed
               ├─ blocklist suffix-match → DnsResponder.blockedResponse()
               │     → tulis ke TUN + counter blocked + log domain
               └─ no match → DnsForwarder.forward() + counter allowed
```

- QTYPE A(1) → 0.0.0.0; AAAA(28) → ::; lainnya → NXDOMAIN (rcode 3). Query ID di-copy persis.
- Wildcard suffix-match: `ads.foo.com` dicocokkan ke `ads.foo.com`, `foo.com` (potong label per level) — semantik `||domain^`.
- **DnsForwarder:** DatagramSocket + `protect(socket)` (wajib — cegah loop TUN), timeout 5s, upstream AdGuard 94.140.14.14.
- Concurrency: `ConcurrentHashMap.newKeySet` untuk sets; `AtomicLong` counters; engine loop single-threaded reader.
- Statistik: `StateFlow<ShieldStats>` (total/allowed/blocked/ignored) + ring buffer blocked log (max 200: domain + timestamp + list penangkap).
- Pause: `AtomicBoolean paused` — semua query di-forward tanpa blocking, TUN tetap hidup.
- Lifecycle: loop dibatalkan di onDestroy; START_STICKY; session tetap "DNS Web Shield".

## 6. Update Lists (FilterListUpdateWorker)

- WorkManager periodik 7 hari; fetch URL resmi uAssets/AdGuard (commit terakhir), HttpURLConnection timeout 30s.
- Simpan ke `filesDir/filters/`; engine prioritas file update, fallback bundel assets (offline-first).
- Hot-reload setelah update sukses tanpa restart VPN (repo version counter).
- Integrity: file > 10KB, hasil parse ≥ 100 rules — jika gagal, buang + fallback bundel.
- Gagal fetch: diabaikan, status update terakhir disimpan untuk dashboard.

## 7. Error Handling

| Kondisi | Penanganan |
|---|---|
| establish() null/exception | stopSelf + log (perilaku existing) |
| Paket rusak/bukan DNS | drop + counter ignored |
| Upstream timeout/socket error | jawab SERVFAIL (rcode 2), tutup socket; socket baru per query |
| List file korup | fallback bundel + log; engine tetap jalan |
| TUN read error/service mati | loop exit bersih; interface ditutup di onDestroy |
| Pause saat query berjalan | query lanjut (flag dibaca per query) |

## 8. Allowlist & Config

- Allowlist domain user: suffix-match, prioritas di atas blocklist; persist SharedPreferences (pola VpnConfigRepositoryImpl existing).
- Custom rules: v1.2 (YAGNI).
- ShieldConfig (paused, enable/disable per list, allowlist): SharedPreferences JSON.

## 9. UI Dashboard (ShieldActivity)

Entry: kartu "DNS-LEVEL WEB SHIELD" existing di MainActivity → buka ShieldActivity (dialog VPN existing dipertahankan sebagai jalur permission alternatif).

Layout (brutalism, komponen Omnix existing, aksen FeatureType.SHIELD cyan):
- Header: "WEB SHIELD" + status dot (AKTIF/PAUSED/OFF)
- 01 STATS: 4 StatusCard (TOTAL/ALLOWED/BLOCKED/IGNORED), count-up
- 02 BLOCKED LOG: reuse TerminalLog ("> domain [List]", max 200, blink, empty state)
- 03 LISTS: nama + jumlah rule + toggle + umur update
- 04 ALLOWLIST: input + daftar + hapus
- Control bar: PAUSE/RESUME + START/STOP VPN

Blocked log saja (allowed tidak dilog — YAGNI). Counters refresh per detik saat layar terbuka.

## 10. YAGNI

DoT/DoH upstream, IPv6 routing penuh, custom rules editor, export/import config, per-app exclusion, MITM/filtering konten, GeckoView, blocked-request untuk trafik non-DNS.

## 11. Verifikasi

1. FilterListParser, DnsPacketParser, DnsResponder = fungsi murni (byte[] in/out) — unit-testable; junit 4.13.2 sudah ada.
2. Gate CI: assembleDebug hijau (pola existing).
3. QA device: browsing normal lancar; doubleclick.net (EasyList) terblokir + counter naik; pause/resume instan; allowlist meng-allow domain yang diblokir.

## 12. Kriteria Selesai

- [ ] Layer model/repo/filter/vpn/worker lengkap sesuai struktur §2
- [ ] 3 lists ter-bundel + parser menghasilkan sets ≥ 10k rules gabungan
- [ ] LocalFirewallService narrow-route: DNS terblokir 0.0.0.0, non-DNS lewat normal
- [ ] protect() di semua socket forwarder
- [ ] Dashboard: stats hidup, blocked log, lists toggle, allowlist, pause/resume
- [ ] Update worker terdaftar (7 hari) + fallback bundel bekerja offline
- [ ] CI assembleDebug hijau

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
    @ApplicationContext private val context: Context
) : DnsShieldRepository {

    private val store = FilterListStore(context)
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dns_shield", Context.MODE_PRIVATE)

    private val blockedByList = ConcurrentHashMap<FilterListId, Set<String>>()
    private val allowedByList = ConcurrentHashMap<FilterListId, Set<String>>()
    private val userAllow: MutableSet<String> = ConcurrentHashMap.newKeySet<String>().also { s ->
        prefs.getStringSet(KEY_ALLOW, emptySet())?.forEach { s.add(it.lowercase()) }
    }

    private val _stats = MutableStateFlow(ShieldStats())
    override val stats: StateFlow<ShieldStats> = _stats.asStateFlow()
    private val _blockedLog = MutableStateFlow<List<BlockedLogEntry>>(emptyList())
    override val blockedLog: StateFlow<List<BlockedLogEntry>> = _blockedLog.asStateFlow()
    private val _listStatuses = MutableStateFlow<List<FilterListStatus>>(emptyList())
    override val listStatuses: StateFlow<List<FilterListStatus>> = _listStatuses.asStateFlow()
    private val _paused = MutableStateFlow(prefs.getBoolean(KEY_PAUSED, false))
    override val paused: StateFlow<Boolean> = _paused.asStateFlow()
    private val _lastUpdate = MutableStateFlow<Long?>(prefs.getLong(KEY_LAST_UPDATE, 0L).takeIf { it > 0 })
    override val lastUpdate: StateFlow<Long?> = _lastUpdate.asStateFlow()
    private val _engineVersion = MutableStateFlow(0)
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

    override fun incrementAllowed() { _stats.update { it.copy(allowed = it.allowed + 1) } }

    override fun recordBlocked(domain: String, listTitle: String) {
        _stats.update { it.copy(blocked = it.blocked + 1) }
        _blockedLog.update { current ->
            (listOf(BlockedLogEntry(domain, listTitle, System.currentTimeMillis())) + current).take(200)
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
        prefs.edit().putStringSet(KEY_ENABLED, listEnabled.filterValues { it }.keys.map { it.name }.toSet()).apply()
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
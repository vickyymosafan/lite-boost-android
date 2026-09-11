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
    val allowlist: StateFlow<Set<String>>
    val rulesReady: StateFlow<Boolean>

    /** Keputusan untuk satu domain. Dipanggil engine loop per query. */
    fun checkDomain(domain: String): ShieldDecision
    fun incrementTotal()
    fun incrementIgnored()
    fun incrementAllowed()
    fun recordBlocked(domain: String, listTitle: String)
    fun allowlistSnapshot(): List<String>
    fun reloadLists(changed: FilterListId? = null)
    suspend fun updateLists(): Boolean
    fun setPaused(value: Boolean)
    fun setListEnabled(id: FilterListId, enabled: Boolean)
    fun addAllowDomain(domain: String)
    fun removeAllowDomain(domain: String)
}
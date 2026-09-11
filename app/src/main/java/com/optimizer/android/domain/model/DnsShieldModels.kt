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
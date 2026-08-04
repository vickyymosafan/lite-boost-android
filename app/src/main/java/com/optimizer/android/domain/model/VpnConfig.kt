package com.optimizer.android.domain.model

data class VpnConfig(
    val sessionName: String = "DNS Web Shield",
    val localAddress: String = "10.0.0.2",
    val localPrefixLength: Int = 24,
    val dnsServers: List<String> = listOf("94.140.14.14", "94.140.15.15"),
    val routes: List<String> = listOf("94.140.14.14", "94.140.15.15"),
    val routePrefixLength: Int = 32
)

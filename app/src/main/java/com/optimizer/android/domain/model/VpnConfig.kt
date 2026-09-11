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

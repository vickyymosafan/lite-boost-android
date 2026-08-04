package com.optimizer.android.data.repository

import com.optimizer.android.domain.model.VpnConfig
import com.optimizer.android.domain.repository.VpnConfigRepository
import javax.inject.Inject

class VpnConfigRepositoryImpl @Inject constructor() : VpnConfigRepository {
    override suspend fun getVpnConfig(): VpnConfig = withContext(Dispatchers.IO) {
        return@withContext VpnConfig(
            sessionName = "DNS Web Shield",
            localAddress = "10.0.0.2",
            localPrefixLength = 24,
            dnsServers = listOf("94.140.14.14", "94.140.15.15"),
            routes = listOf("94.140.14.14", "94.140.15.15"),
            routePrefixLength = 32
        )
    }
}

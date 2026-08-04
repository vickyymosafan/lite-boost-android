package com.optimizer.android.domain.repository

import com.optimizer.android.domain.model.VpnConfig

interface VpnConfigRepository {
    suspend fun getVpnConfig(): VpnConfig
}

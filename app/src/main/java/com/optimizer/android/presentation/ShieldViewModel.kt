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
    val allowlist = dnsShieldRepository.allowlist
    val rulesReady = dnsShieldRepository.rulesReady
    val vpnRunning = LocalFirewallService.running

    fun refreshLists() = dnsShieldRepository.reloadLists()

    fun updateLists() {
        viewModelScope.launch { dnsShieldRepository.updateLists() }
    }

    fun setPaused(value: Boolean) = dnsShieldRepository.setPaused(value)

    fun setListEnabled(id: FilterListId, value: Boolean) = dnsShieldRepository.setListEnabled(id, value)

    fun addAllow(domain: String) = dnsShieldRepository.addAllowDomain(domain)

    fun removeAllow(domain: String) = dnsShieldRepository.removeAllowDomain(domain)

    fun allowlistSnapshot(): List<String> = dnsShieldRepository.allowlistSnapshot()
}

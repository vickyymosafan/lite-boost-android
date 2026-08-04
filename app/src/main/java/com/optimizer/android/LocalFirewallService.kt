package com.optimizer.android

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.optimizer.android.domain.model.VpnConfig

class LocalFirewallService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val vpnConfig = VpnConfig() // Default AdGuard DNS config

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopVPN()
            return START_NOT_STICKY
        }
        startVPN()
        return START_STICKY
    }

    private fun startVPN() {
        if (vpnInterface == null) {
            val builder = Builder()
            builder.setSession(vpnConfig.sessionName)
            builder.addAddress(vpnConfig.localAddress, vpnConfig.localPrefixLength)
            
            vpnConfig.dnsServers.forEach { dns ->
                builder.addDnsServer(dns)
            }
            
            vpnConfig.routes.forEach { route ->
                builder.addRoute(route, vpnConfig.routePrefixLength)
            }
            
            try {
                vpnInterface = builder.establish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopVPN() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVPN()
    }
}

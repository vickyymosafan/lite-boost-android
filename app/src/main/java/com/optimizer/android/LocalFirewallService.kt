package com.optimizer.android

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class LocalFirewallService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

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
            builder.setSession("DNS Web Shield")
            // Gunakan AdGuard DNS (Memblokir Iklan & Malware)
            builder.addAddress("10.0.0.2", 24)
            builder.addDnsServer("94.140.14.14")
            builder.addDnsServer("94.140.15.15")
            // Route DNS requests melalui VPN
            builder.addRoute("94.140.14.14", 32)
            builder.addRoute("94.140.15.15", 32)
            
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

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
            builder.setSession("Local Firewall")
            // Route all IPv4 traffic to nowhere (Blackhole)
            builder.addRoute("0.0.0.0", 0)
            // Note: In a real advanced app, we would use addDisallowedApplication to let system apps bypass
            
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

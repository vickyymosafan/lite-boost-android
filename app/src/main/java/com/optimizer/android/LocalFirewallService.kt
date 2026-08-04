package com.optimizer.android

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.optimizer.android.domain.repository.VpnConfigRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context

@AndroidEntryPoint
class LocalFirewallService : VpnService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    @Inject
    lateinit var vpnConfigRepository: VpnConfigRepository

    private var vpnInterface: ParcelFileDescriptor? = null

    companion object {
        private const val ACTION_START = "com.optimizer.android.ACTION_START"
        private const val ACTION_STOP = "com.optimizer.android.ACTION_STOP"
        private const val TAG = "LocalFirewallService"
        
        fun startIntent(context: Context): Intent = Intent(context, LocalFirewallService::class.java).apply { action = ACTION_START }
        fun stopIntent(context: Context): Intent = Intent(context, LocalFirewallService::class.java).apply { action = ACTION_STOP }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                closeVpnInterface()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startVPN()
                return START_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun startVPN() {
        if (vpnInterface == null) {
            scope.launch {
                val vpnConfig = vpnConfigRepository.getVpnConfig()
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
                    if (vpnInterface == null) {
                        Log.e(TAG, "Failed to establish VPN: permission denied or not prepared")
                        stopSelf()
                    } else {
                        Log.i(TAG, "VPN established successfully")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to establish VPN interface", e)
                    stopSelf()
                }
            }
        }
    }

    private fun closeVpnInterface() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            Log.i(TAG, "VPN closed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }
    }

    override fun onDestroy() {
        closeVpnInterface()
        job.cancel()
        super.onDestroy()
    }
}

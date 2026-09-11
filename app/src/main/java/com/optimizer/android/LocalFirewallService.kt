package com.optimizer.android

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.optimizer.android.data.vpn.DnsForwarder
import com.optimizer.android.data.vpn.DnsPacketParser
import com.optimizer.android.data.vpn.DnsResponder
import com.optimizer.android.data.vpn.IpPacket
import com.optimizer.android.domain.model.ShieldDecision
import com.optimizer.android.domain.repository.DnsShieldRepository
import com.optimizer.android.domain.repository.VpnConfigRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class LocalFirewallService : VpnService() {

    @Inject lateinit var vpnConfigRepository: VpnConfigRepository
    @Inject lateinit var dnsShieldRepository: DnsShieldRepository

    private var vpnInterface: ParcelFileDescriptor? = null
    private var engineJob: Job? = null
    private var tunInput: FileInputStream? = null
    private var tunOutput: FileOutputStream? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val forwarder by lazy { DnsForwarder(this) }
    private val writeMutex = kotlinx.coroutines.sync.Mutex()
    private val inFlight = kotlinx.coroutines.sync.Semaphore(16)

    companion object {
        private const val ACTION_START = "com.optimizer.android.ACTION_START"
        private const val ACTION_STOP = "com.optimizer.android.ACTION_STOP"
        private const val TAG = "LocalFirewallService"
        val running = MutableStateFlow(false)

        fun startIntent(context: Context): Intent = Intent(context, LocalFirewallService::class.java).apply { action = ACTION_START }
        fun stopIntent(context: Context): Intent = Intent(context, LocalFirewallService::class.java).apply { action = ACTION_STOP }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { closeVpnInterface(); stopSelf(); return START_NOT_STICKY }
            ACTION_START -> { startVPN(); return START_STICKY }
        }
        return START_NOT_STICKY
    }

    private fun startVPN() {
        if (vpnInterface == null) {
            scope.launch {
                val vpnConfig = vpnConfigRepository.getVpnConfig()
                val builder = Builder()
                    .setSession(vpnConfig.sessionName)
                    .addAddress(vpnConfig.localAddress, vpnConfig.localPrefixLength)
                vpnConfig.dnsServers.forEach { builder.addDnsServer(it) }
                vpnConfig.routes.forEach { builder.addRoute(it, vpnConfig.routePrefixLength) }
                try {
                    vpnInterface = builder.establish()
                    if (vpnInterface == null) {
                        Log.e(TAG, "Failed to establish VPN: permission denied or not prepared")
                        stopSelf()
                    } else {
                        Log.i(TAG, "VPN established — DNS Shield engine online")
                        runEngine()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to establish VPN interface", e)
                    stopSelf()
                }
            }
        }
    }

    private fun runEngine() {
        val tun = vpnInterface ?: return
        val input = FileInputStream(tun.fileDescriptor)
        val output = FileOutputStream(tun.fileDescriptor)
        tunInput = input; tunOutput = output
        running.value = true
        engineJob = ioScope.launch {
            val buffer = ByteArray(32768)
            while (isActive) {
                try {
                    val n = input.read(buffer)
                    if (n <= 0) break
                    processPacket(buffer, n, output)
                } catch (e: Exception) {
                    Log.w(TAG, "Engine read error", e)
                    break
                }
            }
            running.value = false
        }
    }

    private fun processPacket(buffer: ByteArray, n: Int, output: FileOutputStream) {
        val raw = buffer.copyOf(n)
        val pkt = IpPacket.parseUdp(raw)
        if (pkt == null || pkt.destPort != 53) {
            dnsShieldRepository.incrementIgnored()
            return
        }
        val query = DnsPacketParser.parse(pkt.payload)
        if (query == null) {
            dnsShieldRepository.incrementIgnored()
            return
        }
        dnsShieldRepository.incrementTotal()
        val decision = if (dnsShieldRepository.paused.value) ShieldDecision.Allow
                       else dnsShieldRepository.checkDomain(query.domain)
        when (decision) {
            is ShieldDecision.Block -> {
                val dnsResp = DnsResponder.blockedResponse(query)
                writeMutex.withLock { output.write(IpPacket.buildUdp4Response(pkt, dnsResp)) }
                dnsShieldRepository.recordBlocked(query.domain, decision.listTitle)
            }
            ShieldDecision.Allow -> {
                val pktRef = pkt
                val queryRef = query
                ioScope.launch {
                    inFlight.withPermit {
                        val upstream = forwarder.forward(pktRef.payload)
                        val dnsPayload = upstream ?: DnsResponder.servfailResponse(queryRef)
                        val response = IpPacket.buildUdp4Response(pktRef, dnsPayload)
                        writeMutex.withLock { output.write(response) }
                        dnsShieldRepository.incrementAllowed()
                    }
                }
            }
        }
    }

    private fun closeVpnInterface() {
        try {
            engineJob?.cancel(); engineJob = null
            tunInput?.close(); tunInput = null
            tunOutput?.close(); tunOutput = null
            vpnInterface?.close(); vpnInterface = null
            running.value = false
            Log.i(TAG, "VPN closed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }
    }

    override fun onDestroy() {
        closeVpnInterface()
        scope.cancel(); ioScope.cancel()
        super.onDestroy()
    }
}

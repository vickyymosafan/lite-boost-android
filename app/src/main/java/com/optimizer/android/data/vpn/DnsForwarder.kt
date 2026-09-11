package com.optimizer.android.data.vpn

import android.net.VpnService
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DnsForwarder(private val vpnService: VpnService) {

    companion object {
        private const val UPSTREAM = "94.140.14.14"
        private const val UPSTREAM_PORT = 53
        private const val TIMEOUT_MS = 5000
    }

    /** Kirim query ke upstream AdGuard, balikkan payload jawaban. Null = gagal/timeout. */
    fun forward(payload: ByteArray): ByteArray? {
        val socket = DatagramSocket()
        return try {
            vpnService.protect(socket) // wajib — tanpa ini socket masuk TUN lagi
            socket.soTimeout = TIMEOUT_MS
            val addr = InetAddress.getByName(UPSTREAM)
            socket.send(DatagramPacket(payload, payload.size, addr, UPSTREAM_PORT))
            val buf = ByteArray(4096)
            val recv = DatagramPacket(buf, buf.size)
            socket.receive(recv)
            recv.data.copyOf(recv.length)
        } catch (e: Exception) {
            null
        } finally {
            socket.close()
        }
    }
}

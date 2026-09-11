package com.optimizer.android.data.vpn

import android.net.VpnService
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

class DnsForwarder(private val vpnService: VpnService) {

    companion object {
        private const val TIMEOUT_MS = 3000

        // UDP upstreams — dicoba berurutan
        private val udpUpstreams = listOf(
            "94.140.14.14" to 53,  // AdGuard
            "94.140.15.15" to 53,  // AdGuard 2
            "8.8.8.8" to 53,       // Google
            "1.1.1.1" to 53,       // Cloudflare
            "9.9.9.9" to 53        // Quad9
        )

        // DoH upstreams — dicoba kalau semua UDP gagal
        private val dohUpstreams = listOf(
            "https://dns.adguard.com/dns-query",
            "https://cloudflare-dns.com/dns-query",
            "https://dns.google/dns-query"
        )
    }

    fun forward(payload: ByteArray): ByteArray? {
        // 1. Coba UDP upstreams
        for ((host, port) in udpUpstreams) {
            val result = tryUdp(host, port, payload)
            if (result != null) return result
        }
        // 2. Fallback ke DoH
        for (url in dohUpstreams) {
            val result = tryDoh(url, payload)
            if (result != null) return result
        }
        return null // semua gagal
    }

    private fun tryUdp(host: String, port: Int, payload: ByteArray): ByteArray? {
        val socket = DatagramSocket()
        return try {
            vpnService.protect(socket)
            socket.soTimeout = TIMEOUT_MS
            val addr = InetAddress.getByName(host)
            socket.send(DatagramPacket(payload, payload.size, addr, port))
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

    private fun tryDoh(url: String, payload: ByteArray): ByteArray? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/dns-message")
            conn.doOutput = true
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.outputStream.write(payload)
            val responseCode = conn.responseCode
            if (responseCode != 200) return null
            val input = conn.inputStream
            val buffer = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            var n: Int
            while (input.read(buf).also { n = it } != -1) {
                buffer.write(buf, 0, n)
            }
            buffer.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}

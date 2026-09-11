package com.optimizer.android.data.vpn

data class DnsQuery(val id: Int, val domain: String, val type: Int)

object DnsPacketParser {

    /** Payload DNS (tanpa header IP/UDP). Null jika bukan query valid. */
    fun parse(payload: ByteArray): DnsQuery? {
        if (payload.size < 17) return null
        val flags = ((payload[2].toInt() and 0xFF) shl 8) or (payload[3].toInt() and 0xFF)
        if (flags and 0x8000 != 0) return null // QR=1 → response, bukan query
        val qdcount = ((payload[4].toInt() and 0xFF) shl 8) or (payload[5].toInt() and 0xFF)
        if (qdcount < 1) return null
        val id = ((payload[0].toInt() and 0xFF) shl 8) or (payload[1].toInt() and 0xFF)
        var off = 12
        val labels = ArrayList<String>(8)
        var nameLen = 0
        while (true) {
            if (off >= payload.size) return null
            val len = payload[off].toInt() and 0xFF
            if (len == 0) { off += 1; break }
            if (len and 0xC0 != 0) return null // compression pointer di question: tolak
            if (off + 1 + len > payload.size) return null
            nameLen += len + 1
            if (nameLen > 255) return null
            labels.add(String(payload, off + 1, len, Charsets.US_ASCII))
            off += 1 + len
        }
        if (labels.isEmpty()) return null
        if (off + 4 > payload.size) return null
        val type = ((payload[off].toInt() and 0xFF) shl 8) or (payload[off + 1].toInt() and 0xFF)
        return DnsQuery(id, labels.joinToString(".").lowercase(), type)
    }
}
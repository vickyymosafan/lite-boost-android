package com.optimizer.android.data.vpn

data class IpUdpPacket(
    val sourceIp: ByteArray,
    val destIp: ByteArray,
    val sourcePort: Int,
    val destPort: Int,
    val payload: ByteArray
)

object IpPacket {

    /** Parse paket IPv4+UDP mentah dari TUN. Null jika bukan IPv4/UDP/rusak. */
    fun parseUdp(raw: ByteArray): IpUdpPacket? {
        if (raw.size < 28) return null
        if (((raw[0].toInt() shr 4) and 0xF) != 4) return null
        val ihl = (raw[0].toInt() and 0xF) * 4
        if (raw.size < ihl + 8) return null
        if ((raw[9].toInt() and 0xFF) != 17) return null
        val src = raw.copyOfRange(12, 16)
        val dst = raw.copyOfRange(16, 20)
        val sport = ((raw[ihl].toInt() and 0xFF) shl 8) or (raw[ihl + 1].toInt() and 0xFF)
        val dport = ((raw[ihl + 2].toInt() and 0xFF) shl 8) or (raw[ihl + 3].toInt() and 0xFF)
        val payload = raw.copyOfRange(ihl + 8, raw.size)
        return IpUdpPacket(src, dst, sport, dport, payload)
    }

    /** Bangun respons IPv4+UDP dengan src/dst dibalik dari paket asli. */
    fun buildUdp4Response(orig: IpUdpPacket, payload: ByteArray): ByteArray {
        val totalLen = 20 + 8 + payload.size
        val out = ByteArray(totalLen)
        out[0] = 0x45
        out[2] = ((totalLen shr 8) and 0xFF).toByte()
        out[3] = (totalLen and 0xFF).toByte()
        out[6] = 0x40 // DF
        out[8] = 64   // TTL
        out[9] = 17   // UDP
        for (i in 0..3) {
            out[12 + i] = orig.destIp[i]   // new src = orig dst
            out[16 + i] = orig.sourceIp[i] // new dst = orig src
        }
        val u = 20
        out[u] = ((orig.destPort shr 8) and 0xFF).toByte()
        out[u + 1] = (orig.destPort and 0xFF).toByte()
        out[u + 2] = ((orig.sourcePort shr 8) and 0xFF).toByte()
        out[u + 3] = (orig.sourcePort and 0xFF).toByte()
        val udpLen = 8 + payload.size
        out[u + 4] = ((udpLen shr 8) and 0xFF).toByte()
        out[u + 5] = (udpLen and 0xFF).toByte()
        out[u + 6] = 0; out[u + 7] = 0 // UDP checksum 0 valid utk IPv4
        payload.copyInto(out, 28)
        val sum = checksum(out, 0, 20)
        out[10] = ((sum shr 8) and 0xFF).toByte()
        out[11] = (sum and 0xFF).toByte()
        return out
    }

    fun checksum(data: ByteArray, offset: Int, count: Int): Int {
        var sum = 0L
        var i = offset
        val end = offset + count
        while (i < end - 1) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (count % 2 == 1) sum += (data[end - 1].toInt() and 0xFF) shl 8
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv() and 0xFFFF
    }
}
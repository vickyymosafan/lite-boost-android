package com.optimizer.android.data.vpn

object DnsResponder {

    private const val RCODE_NXDOMAIN = 3
    private const val RCODE_SERVFAIL = 2
    private const val TYPE_A = 1
    private const val TYPE_AAAA = 28

    /** A → 0.0.0.0, AAAA → ::, selain itu → NXDOMAIN. */
    fun blockedResponse(query: DnsQuery): ByteArray = when (query.type) {
        TYPE_A -> withAnswer(query, 4, ByteArray(4))
        TYPE_AAAA -> withAnswer(query, 16, ByteArray(16))
        else -> nxdomainResponse(query)
    }

    fun servfailResponse(query: DnsQuery): ByteArray = withRcodeOnly(query, RCODE_SERVFAIL)

    fun nxdomainResponse(query: DnsQuery): ByteArray = withRcodeOnly(query, RCODE_NXDOMAIN)

    fun encodeQuestion(domain: String, type: Int): ByteArray {
        val labels = domain.split('.')
        var size = 1 + labels.sumOf { it.length + 1 } + 4
        val out = ByteArray(size)
        var off = 0
        for (label in labels) {
            val bytes = label.toByteArray(Charsets.US_ASCII)
            out[off] = bytes.size.toByte()
            bytes.copyInto(out, off + 1)
            off += 1 + bytes.size
        }
        out[off] = 0
        off += 1
        out[off] = ((type shr 8) and 0xFF).toByte()
        out[off + 1] = (type and 0xFF).toByte()
        out[off + 2] = 0; out[off + 3] = 1 // IN
        return out
    }

    private fun withAnswer(query: DnsQuery, rdataLen: Int, rdata: ByteArray): ByteArray {
        val q = encodeQuestion(query.domain, query.type)
        val size = 12 + q.size + 12 + rdataLen
        val out = ByteArray(size)
        out[0] = ((query.id shr 8) and 0xFF).toByte()
        out[1] = (query.id and 0xFF).toByte()
        out[2] = 0x81.toByte(); out[3] = 0x80.toByte() // QR=1 RD=1 RA=1 rcode=0
        out[5] = 1 // QDCOUNT=1
        out[7] = 1 // ANCOUNT=1
        encodeQuestion(query.domain, query.type).copyInto(out, 12)
        var off = 12 + q.size
        out[off] = 0xC0.toByte(); out[off + 1] = 0x0C // pointer ke QNAME
        out[off + 2] = ((query.type shr 8) and 0xFF).toByte()
        out[off + 3] = (query.type and 0xFF).toByte()
        out[off + 4] = 0; out[off + 5] = 1 // IN
        out[off + 6] = 0; out[off + 7] = 0; out[off + 8] = 0; out[off + 9] = 60 // TTL 60s
        out[off + 10] = ((rdataLen shr 8) and 0xFF).toByte()
        out[off + 11] = (rdataLen and 0xFF).toByte()
        return out
    }

    private fun withRcodeOnly(query: DnsQuery, rcode: Int): ByteArray {
        val q = encodeQuestion(query.domain, query.type)
        val out = ByteArray(12 + q.size)
        out[0] = ((query.id shr 8) and 0xFF).toByte()
        out[1] = (query.id and 0xFF).toByte()
        out[2] = 0x81.toByte()
        out[3] = (0x80 or rcode).toByte() // QR=1 RD=1 RA=1 + rcode
        out[5] = 1 // QDCOUNT=1, ANCOUNT=0
        q.copyInto(out, 12)
        return out
    }
}
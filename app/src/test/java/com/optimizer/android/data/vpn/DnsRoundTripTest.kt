package com.optimizer.android.data.vpn

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DnsRoundTripTest {

    private fun buildQueryBytes(domain: String, type: Int, id: Int = 0x1234): ByteArray {
        val q = DnsResponder.encodeQuestion(domain, type)
        val out = ByteArray(12 + q.size)
        out[0] = ((id shr 8) and 0xFF).toByte(); out[1] = (id and 0xFF).toByte()
        out[5] = 1 // QDCOUNT=1
        q.copyInto(out, 12)
        return out
    }

    @Test
    fun parsesValidQuery() {
        val q = DnsPacketParser.parse(buildQueryBytes("ads.example.com", 1))
        assertEquals("ads.example.com", q?.domain)
        assertEquals(1, q?.type)
        assertEquals(0x1234, q?.id)
    }

    @Test
    fun rejectsGarbageAndResponses() {
        assertNull(DnsPacketParser.parse(byteArrayOf(1, 2, 3)))
        val resp = buildQueryBytes("x.com", 1).clone()
        resp[2] = resp[2].or(0x80.toByte()) // QR=1
        assertNull(DnsPacketParser.parse(resp))
    }

    @Test
    fun blockedResponseAIsZeroIp() {
        val q = DnsQuery(0x1234, "ads.example.com", 1)
        val resp = DnsResponder.blockedResponse(q)
        assertEquals(0x1234, ((resp[0].toInt() and 0xFF) shl 8) or (resp[1].toInt() and 0xFF))
        assertEquals(1, resp[7].toInt()) // ANCOUNT=1
        val rdStart = resp.size - 4
        assertArrayEquals(ByteArray(4), resp.copyOfRange(rdStart, rdStart + 4))
    }

    @Test
    fun ipPacketRoundTrip() {
        val dnsPayload = buildQueryBytes("test.org", 28)
        val orig = ByteArray(28 + dnsPayload.size)
        orig[0] = 0x45
        orig[9] = 17
        for (i in 0..3) { orig[12 + i] = 10; orig[16 + i] = 20 } // src=10.x dst=20.x
        orig[20] = 0x30; orig[21] = 0x39 // sport 12345
        orig[22] = 0; orig[23] = 53      // dport 53
        dnsPayload.copyInto(orig, 28)
        val pkt = IpPacket.parseUdp(orig)
        assertEquals(12345, pkt?.sourcePort)
        assertEquals(53, pkt?.destPort)
        val resp = IpPacket.buildUdp4Response(pkt!!, DnsResponder.blockedResponse(DnsQuery(1, "test.org", 28)))
        // src/dst tertukar: new src (byte 12..15) = orig dst = 20.x, new dst (byte 16..19) = orig src = 10.x
        assertEquals(20, resp[12].toInt() and 0xFF)
        assertEquals(10, resp[16].toInt() and 0xFF)
        assertNull(IpPacket.parseUdp(byteArrayOf(0, 1, 2)))
    }
}
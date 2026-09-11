package com.optimizer.android.data.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterListParserTest {

    @Test
    fun parsesAdblockDomainRule() {
        val r = FilterListParser.parse("||ads.example.com^\n")
        assertTrue(r.blocked.contains("ads.example.com"))
        assertEquals(1, r.ruleCount)
    }

    @Test
    fun parsesExceptionRule() {
        val r = FilterListParser.parse("@@||ok.example.com^\n||ads.example.com^\n")
        assertTrue(r.allowed.contains("ok.example.com"))
        assertTrue(r.blocked.contains("ads.example.com"))
    }

    @Test
    fun parsesHostsRule() {
        val r = FilterListParser.parse("0.0.0.0 tracker.net\n127.0.0.1 spy.io\n")
        assertTrue(r.blocked.contains("tracker.net") && r.blocked.contains("spy.io"))
    }

    @Test
    fun skipsCommentsAndComplexRules() {
        val r = FilterListParser.parse("! comment\n# anchor\n/banner\\d+/\nexample.com##.cls\n||real.domain^\n")
        assertTrue(r.blocked.contains("real.domain"))
        assertEquals(1, r.ruleCount)
    }

    @Test
    fun suffixMatchCoversSubdomains() {
        val sets = setOf("example.com")
        assertTrue(FilterListParser.checkDomain(sets, "example.com"))
        assertTrue(FilterListParser.checkDomain(sets, "ads.example.com"))
        assertTrue(FilterListParser.checkDomain(sets, "a.b.example.com"))
        assertFalse(FilterListParser.checkDomain(sets, "notexample.com"))
        assertFalse(FilterListParser.checkDomain(sets, "example.org"))
    }
}
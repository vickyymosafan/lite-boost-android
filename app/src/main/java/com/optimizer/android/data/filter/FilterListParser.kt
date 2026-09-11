package com.optimizer.android.data.filter

object FilterListParser {

    data class Result(val blocked: Set<String>, val allowed: Set<String>, val ruleCount: Int)

    fun parse(content: String): Result {
        val blocked = HashSet<String>()
        val allowed = HashSet<String>()
        var count = 0
        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("!") || line.startsWith("#") || line.startsWith("[")) return@forEach
            val isException = line.startsWith("@@")
            val body = if (isException) line.substring(2) else line

            // Hosts style: 0.0.0.0 domain
            val parts = body.split(Regex("\\s+"))
            if (parts.size == 2 && (parts[0] == "0.0.0.0" || parts[0] == "127.0.0.1")) {
                val d = parts[1].lowercase().trimEnd('.')
                if (d.isNotEmpty()) {
                    if (isException) allowed.add(d) else blocked.add(d)
                    count++
                }
                return@forEach
            }

            // Adblock-style: ||domain^  (exception: @@||domain^)
            if (body.startsWith("||")) {
                var domain = body.substring(2)
                val cut = domain.indexOfFirst { it == '^' || it == '$' || it == '/' || it == '*' }
                if (cut >= 0) domain = domain.substring(0, cut)
                domain = domain.lowercase().trimEnd('.')
                if (domain.isEmpty() || domain.contains("/") || domain.contains(" ")) return@forEach
                if (isException) allowed.add(domain) else blocked.add(domain)
                count++
            }
        }
        return Result(blocked, allowed, count)
    }

    /** Suffix match: domain itu sendiri atau salah satu parent-nya ada di set. */
    fun checkDomain(sets: Set<String>, domain: String): Boolean {
        if (sets.contains(domain)) return true
        var idx = domain.indexOf('.')
        while (idx > 0) {
            if (sets.contains(domain.substring(idx + 1))) return true
            idx = domain.indexOf('.', idx + 1)
        }
        return false
    }
}
package com.base.app.buildlogic.guard

import com.base.app.buildlogic.guard.CodeRules.Finding

/**
 * Findings accepted in `config/code-rules-baseline.txt`, one `<path> <rule> <count>` line each:
 * code written before its rule existed, waiting for its owner. A file may break a listed rule at
 * most that many times. Counts rather than line numbers, so editing the file does not churn the
 * list, and an entry that has become too high fails too, so the list only ever shrinks.
 *
 * Paths are compared without a modular project's `api`/`impl` level, so one list serves a project
 * before and after `modes.py` splits it.
 */
internal class CodeRulesBaseline(accepted: Map<Pair<String, String>, Int>) {

    private val accepted = accepted.mapKeys { (key, _) -> compact(key.first) to key.second }

    /**
     * What to report for the scanned files, [findings] by path: every finding beyond its file's
     * accepted count, and every entry for one of them that now accepts more than the file breaks.
     */
    fun judge(findings: Map<String, List<Finding>>): List<String> {
        val reports = mutableListOf<String>()
        val scanned = findings.mapKeys { (path, _) -> compact(path) }
        findings.forEach { (path, found) ->
            found.groupBy { it.rule }.forEach { (rule, group) ->
                val allowed = accepted[compact(path) to rule] ?: 0
                if (group.size >
                    allowed
                ) {
                    reports += group.map { "$path:${it.line} ${it.rule}: ${it.message}" }
                }
            }
        }
        accepted.forEach { (key, allowed) ->
            val (path, rule) = key
            val found = scanned[path] ?: return@forEach
            val count = found.count { it.rule == rule }
            if (count < allowed) {
                reports +=
                    "$path $rule: $FILE accepts $allowed, the file now has $count; lower the count or delete the line"
            }
        }
        return reports
    }

    companion object {
        const val FILE = "config/code-rules-baseline.txt"

        val EMPTY = CodeRulesBaseline(emptyMap())

        private val ENTRY = Regex("""^(\S+)\s+([a-z-]+)\s+(\d+)$""")
        private val SPLIT_MODULE = Regex("""^((?:feature|data)/[^/]+)/(?:api|impl)/""")

        private fun compact(path: String): String = path.replace(SPLIT_MODULE, "$1/")

        fun parse(text: String): CodeRulesBaseline = CodeRulesBaseline(
            text.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .associate { line ->
                    val match =
                        requireNotNull(ENTRY.matchEntire(line)) {
                            "$FILE: '$line' is not '<path> <rule> <count>'"
                        }
                    (match.groupValues[1] to match.groupValues[2]) to
                        match.groupValues[3].toInt()
                },
        )
    }
}

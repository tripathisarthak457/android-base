package com.example.hud.lean.session

import com.example.hud.lean.core.datastore.session.SESSION_TOKENS_FILE
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class SessionBackupRulesTest {

    @Test
    fun `the session's tokens stay out of every backup and device transfer`() {
        val tokens = "datastore/$SESSION_TOKENS_FILE.preferences_pb"
        val sections = excludedFiles("backup_rules") + excludedFiles("data_extraction_rules")

        assertEquals(
            setOf("full-backup-content", "cloud-backup", "device-transfer"),
            sections.filterValues { tokens in it }.keys,
        )
    }

    /** The `file` paths each section of a rules file excludes, by the section's tag. */
    private fun excludedFiles(rules: String): Map<String, Set<String>> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/xml/$rules.xml"))
        val excludes = document.getElementsByTagName("exclude")
        return (0 until excludes.length)
            .map { excludes.item(it) as Element }
            .filter { it.getAttribute("domain") == "file" }
            .groupBy({ (it.parentNode as Element).tagName }, { it.getAttribute("path") })
            .mapValues { (_, paths) -> paths.toSet() }
    }
}

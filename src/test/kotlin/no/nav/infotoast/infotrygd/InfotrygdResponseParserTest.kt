package no.nav.infotoast.infotrygd

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for InfotrygdResponseParser
 *
 * TODO: Add tests with actual Infotrygd response examples once we have real XML format The current
 *   tests only verify error handling for malformed/empty XML.
 */
class InfotrygdResponseParserTest {

    private val parser = InfotrygdResponseParser()

    @Test
    fun `should handle malformed XML gracefully`() {
        val xml = "<invalid>xml</not-matching>"

        val response = parser.parseSporringResponse(xml)

        // Parser returns empty response (null values) on parse failure
        assertNull(response.identDato, "Should return null identDato for malformed XML")
        assertNull(response.tkNummer, "Should return null tkNummer for malformed XML")
    }

    @Test
    fun `should handle empty XML`() {
        val xml = ""

        val response = parser.parseSporringResponse(xml)

        // Parser returns empty response (null values) on parse failure
        assertNull(response.identDato, "Should return null identDato for empty XML")
        assertNull(response.tkNummer, "Should return null tkNummer for empty XML")
    }

    @Test
    fun `should handle XML with empty InfotrygdForesp`() {
        val xml =
            """<?xml version="1.0" encoding="UTF-8"?>
            <InfotrygdForesp>
            </InfotrygdForesp>
        """
                .trimIndent()

        val response = parser.parseSporringResponse(xml)

        // Empty response should have null values
        assertNull(response.identDato, "Should return null identDato for empty response")
        assertNull(response.tkNummer, "Should return null tkNummer for empty response")
    }
}

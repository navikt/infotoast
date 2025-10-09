package no.nav.infotoast.infotrygd

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for InfotrygdSporringBuilder
 *
 * Tests the building of sporring (query) messages for Infotrygd
 */
class InfotrygdSporringBuilderTest {

    private lateinit var builder: InfotrygdSporringBuilder

    @BeforeEach
    fun setup() {
        builder = InfotrygdSporringBuilder()
    }

    @Test
    fun `should build sporring message with complete data`() {
        // Given
        val pasientFnr = "12345678901"
        val behandlerFnr = "98765432109"
        val tssId = "12345"
        val sykmeldingId = "test-sykmelding-123"
        val hovedDiagnoseKode = "L87"
        val hovedDiagnoseKodeverk = "ICPC-2"
        val biDiagnoseKode = "L88"
        val biDiagnoseKodeverk = "ICPC-2"

        // When
        val result =
            builder.buildSporringMessage(
                pasientFnr = pasientFnr,
                behandlerFnr = behandlerFnr,
                tssId = tssId,
                sykmeldingId = sykmeldingId,
                hovedDiagnoseKode = hovedDiagnoseKode,
                hovedDiagnoseKodeverk = hovedDiagnoseKodeverk,
                biDiagnoseKode = biDiagnoseKode,
                biDiagnoseKodeverk = biDiagnoseKodeverk
            )

        // Then
        assertNotNull(result)
        assertTrue(result.contains(pasientFnr), "Should contain pasient FNR")
        assertTrue(result.contains(behandlerFnr), "Should contain behandler FNR")
        assertTrue(result.contains(tssId), "Should contain TSS ID")
        assertTrue(result.contains(hovedDiagnoseKode), "Should contain hoveddiagnose code")
        assertTrue(result.contains(biDiagnoseKode), "Should contain bidiagnose code")
    }

    @Test
    fun `should build sporring message without bidiagnose`() {
        // Given
        val pasientFnr = "12345678901"
        val behandlerFnr = "98765432109"
        val tssId = "12345"
        val sykmeldingId = "test-sykmelding-123"
        val hovedDiagnoseKode = "L87"
        val hovedDiagnoseKodeverk = "ICPC-2"

        // When
        val result =
            builder.buildSporringMessage(
                pasientFnr = pasientFnr,
                behandlerFnr = behandlerFnr,
                tssId = tssId,
                sykmeldingId = sykmeldingId,
                hovedDiagnoseKode = hovedDiagnoseKode,
                hovedDiagnoseKodeverk = hovedDiagnoseKodeverk,
                biDiagnoseKode = null,
                biDiagnoseKodeverk = null
            )

        // Then
        assertNotNull(result)
        assertTrue(result.contains(pasientFnr))
        assertTrue(result.contains(hovedDiagnoseKode))
    }

    @Test
    fun `should build sporring message without diagnosis codes`() {
        // Given
        val pasientFnr = "12345678901"
        val behandlerFnr = "98765432109"
        val tssId = "12345"
        val sykmeldingId = "test-sykmelding-123"

        // When
        val result =
            builder.buildSporringMessage(
                pasientFnr = pasientFnr,
                behandlerFnr = behandlerFnr,
                tssId = tssId,
                sykmeldingId = sykmeldingId,
                hovedDiagnoseKode = null,
                hovedDiagnoseKodeverk = null,
                biDiagnoseKode = null,
                biDiagnoseKodeverk = null
            )

        // Then
        assertNotNull(result)
        assertTrue(result.contains(pasientFnr))
        assertTrue(result.contains(behandlerFnr))
        assertTrue(result.contains(tssId))
    }

    @Test
    fun `should produce valid XML structure`() {
        // Given
        val pasientFnr = "12345678901"
        val behandlerFnr = "98765432109"
        val tssId = "12345"
        val sykmeldingId = "test-sykmelding-123"

        // When
        val result =
            builder.buildSporringMessage(
                pasientFnr = pasientFnr,
                behandlerFnr = behandlerFnr,
                tssId = tssId,
                sykmeldingId = sykmeldingId,
                hovedDiagnoseKode = "L87",
                hovedDiagnoseKodeverk = "ICPC-2",
                biDiagnoseKode = null,
                biDiagnoseKodeverk = null
            )

        // Then
        // Basic XML structure validation
        assertTrue(
            result.startsWith("<?xml") || result.startsWith("<"),
            "Should start with XML declaration or root element"
        )
        assertTrue(result.contains("<") && result.contains(">"), "Should contain XML tags")

        // Count opening and closing tags should match
        val openingTags = result.count { it == '<' }
        val closingTags = result.count { it == '>' }
        assertEquals(openingTags, closingTags, "Opening and closing tags should match")
    }
}

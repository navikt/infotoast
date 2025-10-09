package no.nav.infotoast.infotrygd.mq

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for MockInfotrygdMqService
 *
 * Verifies that the mock MQ service behaves correctly for local/test environments
 */
class MockInfotrygdMqServiceTest {

    private lateinit var mockMqService: MockInfotrygdMqService

    @BeforeEach
    fun setup() {
        mockMqService = MockInfotrygdMqService()
    }

    @Test
    fun `should return correlation ID when sending sporring`() {
        // Given
        val xmlMessage = "<sporring>test</sporring>"
        val sykmeldingId = "test-123"

        // When
        val correlationId = mockMqService.sendInfotrygdSporring(xmlMessage, sykmeldingId)

        // Then
        assertNotNull(correlationId, "Should return a correlation ID")
        assertFalse(correlationId.isEmpty(), "Correlation ID should not be empty")
        assertTrue(correlationId.contains("-"), "Should be a UUID format")
    }

    @Test
    fun `should return correlation ID when sending oppdatering`() {
        // Given
        val xmlMessage = "<oppdatering>test</oppdatering>"
        val sykmeldingId = "test-123"

        // When
        val correlationId = mockMqService.sendInfotrygdOppdatering(xmlMessage, sykmeldingId)

        // Then
        assertNotNull(correlationId, "Should return a correlation ID")
        assertFalse(correlationId.isEmpty(), "Correlation ID should not be empty")
        assertTrue(correlationId.contains("-"), "Should be a UUID format")
    }

    @Test
    fun `should return different correlation IDs for different calls`() {
        // Given
        val xmlMessage = "<sporring>test</sporring>"
        val sykmeldingId1 = "test-123"
        val sykmeldingId2 = "test-456"

        // When
        val correlationId1 = mockMqService.sendInfotrygdSporring(xmlMessage, sykmeldingId1)
        val correlationId2 = mockMqService.sendInfotrygdSporring(xmlMessage, sykmeldingId2)

        // Then
        assertNotEquals(correlationId1, correlationId2, "Should generate unique correlation IDs")
    }

    @Test
    fun `should handle large XML messages`() {
        // Given
        val largeXmlMessage = "<sporring>" + "x".repeat(10000) + "</sporring>"
        val sykmeldingId = "test-123"

        // When
        val correlationId = mockMqService.sendInfotrygdSporring(largeXmlMessage, sykmeldingId)

        // Then
        assertNotNull(correlationId, "Should handle large messages without error")
    }

    @Test
    fun `should handle special characters in sykmelding ID`() {
        // Given
        val xmlMessage = "<sporring>test</sporring>"
        val sykmeldingId = "test-123-æøå-特殊"

        // When
        val correlationId = mockMqService.sendInfotrygdSporring(xmlMessage, sykmeldingId)

        // Then
        assertNotNull(correlationId, "Should handle special characters in sykmelding ID")
    }
}

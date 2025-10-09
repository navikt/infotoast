package no.nav.infotoast.infotrygd

import java.time.LocalDateTime

/**
 * Represents the processing state of a sykmelding in the Infotrygd integration flow This is stored
 * in Valkey to support async processing
 */
data class InfotrygdProcessingState(
    val sykmeldingId: String,
    val journalpostId: String,
    val tssId: String,
    val helsepersonellKategori: String,
    val navKontorNr: String,
    val pasientFnr: String,
    val behandlerFnr: String,

    // Processing state
    val currentStep: ProcessingStep,
    val sporringCorrelationId: String? = null,
    val oppdateringCorrelationId: String? = null,

    // Sporring response data (from Infotrygd query response)
    // Based on syfosminfotrygd - only identDato and tkNummer are needed
    val identDato: String? = null,
    val tkNummer: String? = null,

    // Timestamps
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    // Error tracking
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val lastErrorAt: LocalDateTime? = null
)

enum class ProcessingStep {
    INITIATED, // Initial state when Kafka message is received
    SPORRING_SENT, // Sporring query sent to Infotrygd
    SPORRING_RECEIVED, // Sporring response received and parsed
    OPPDATERING_SENT, // Oppdatering message sent to Infotrygd (fire-and-forget)
    COMPLETED, // Processing completed successfully (oppdatering sent)
    FAILED // Processing failed
}

package no.nav.infotoast.infotrygd

import java.time.Duration
import java.time.LocalDateTime
import no.nav.infotoast.utils.logger
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class InfotrygdProcessingStateService(private val redisTemplate: RedisTemplate<String, Any>) {
    private val logger = logger()

    companion object {
        private const val KEY_PREFIX = "infotrygd:processing:"
        private const val CORRELATION_KEY_PREFIX = "infotrygd:correlation:"
        private val TTL = Duration.ofHours(24) // State expires after 24 hours
        const val MAX_RETRY_COUNT = 3
    }

    /** Store processing state for a sykmelding */
    fun saveState(state: InfotrygdProcessingState) {
        val key = buildKey(state.sykmeldingId)
        redisTemplate.opsForValue().set(key, state, TTL)
        logger.info(
            "Saved processing state for sykmelding ${state.sykmeldingId}, step: ${state.currentStep}"
        )
    }

    /** Retrieve processing state for a sykmelding */
    fun getState(sykmeldingId: String): InfotrygdProcessingState? {
        val key = buildKey(sykmeldingId)
        return redisTemplate.opsForValue().get(key) as? InfotrygdProcessingState
    }

    /** Update the processing step and timestamp */
    fun updateStep(sykmeldingId: String, step: ProcessingStep, correlationId: String? = null) {
        val state =
            getState(sykmeldingId)
                ?: run {
                    logger.error(
                        "Cannot update step: Processing state not found for sykmelding $sykmeldingId"
                    )
                    return
                }

        val updatedState =
            when (step) {
                ProcessingStep.SPORRING_SENT ->
                    state.copy(
                        currentStep = step,
                        sporringCorrelationId = correlationId,
                        updatedAt = LocalDateTime.now()
                    )
                ProcessingStep.OPPDATERING_SENT ->
                    state.copy(
                        currentStep = step,
                        oppdateringCorrelationId = correlationId,
                        updatedAt = LocalDateTime.now()
                    )
                else -> state.copy(currentStep = step, updatedAt = LocalDateTime.now())
            }

        saveState(updatedState)

        // If we have a correlation ID, also store a mapping from correlationId -> sykmeldingId
        correlationId?.let { storeCorrelationMapping(it, sykmeldingId) }
    }

    /** Update state with sporring response data */
    fun updateWithSporringResponse(sykmeldingId: String, identDato: String?, tkNummer: String?) {
        val state =
            getState(sykmeldingId)
                ?: run {
                    logger.error(
                        "Cannot update sporring response: Processing state not found for sykmelding $sykmeldingId"
                    )
                    return
                }

        val updatedState =
            state.copy(identDato = identDato, tkNummer = tkNummer, updatedAt = LocalDateTime.now())

        saveState(updatedState)
        logger.info(
            "Updated sykmelding $sykmeldingId with sporring response data: identDato=$identDato, tkNummer=$tkNummer"
        )
    }

    /** Mark processing as failed with error message */
    fun markFailed(sykmeldingId: String, errorMessage: String) {
        val state =
            getState(sykmeldingId)
                ?: run {
                    logger.error(
                        "Cannot mark failed: Processing state not found for sykmelding $sykmeldingId"
                    )
                    return
                }

        val updatedState =
            state.copy(
                currentStep = ProcessingStep.FAILED,
                errorMessage = errorMessage,
                updatedAt = LocalDateTime.now()
            )

        saveState(updatedState)
        logger.error("Marked sykmelding $sykmeldingId as failed: $errorMessage")
    }

    /** Store mapping from correlation ID to sykmelding ID for async response handling */
    private fun storeCorrelationMapping(correlationId: String, sykmeldingId: String) {
        val key = buildCorrelationKey(correlationId)
        redisTemplate.opsForValue().set(key, sykmeldingId, TTL)
        logger.info("Stored correlation mapping: $correlationId -> $sykmeldingId")
    }

    /** Get sykmelding ID from correlation ID */
    fun getSykmeldingIdByCorrelation(correlationId: String): String? {
        val key = buildCorrelationKey(correlationId)
        return redisTemplate.opsForValue().get(key) as? String
    }

    /** Delete processing state (cleanup after completion) */
    fun deleteState(sykmeldingId: String) {
        val state = getState(sykmeldingId)

        // Delete main state
        val key = buildKey(sykmeldingId)
        redisTemplate.delete(key)

        // Delete correlation mappings
        state?.sporringCorrelationId?.let { redisTemplate.delete(buildCorrelationKey(it)) }
        state?.oppdateringCorrelationId?.let { redisTemplate.delete(buildCorrelationKey(it)) }

        logger.info("Deleted processing state for sykmelding $sykmeldingId")
    }

    /** Check if a sykmelding can be retried based on retry count */
    fun canRetry(sykmeldingId: String): Boolean {
        val state = getState(sykmeldingId) ?: return false
        return state.retryCount < MAX_RETRY_COUNT
    }

    /** Increment retry count and reset to appropriate step for retry */
    fun markForRetry(sykmeldingId: String, retryStep: ProcessingStep): Boolean {
        val state = getState(sykmeldingId) ?: return false

        if (state.retryCount >= MAX_RETRY_COUNT) {
            logger.error("Cannot retry sykmelding $sykmeldingId: Max retry count reached")
            return false
        }

        val updatedState =
            state.copy(
                currentStep = retryStep,
                retryCount = state.retryCount + 1,
                updatedAt = LocalDateTime.now()
            )

        saveState(updatedState)
        logger.info(
            "Marked sykmelding $sykmeldingId for retry ${updatedState.retryCount}/$MAX_RETRY_COUNT, step: $retryStep"
        )
        return true
    }

    private fun buildKey(sykmeldingId: String) = "$KEY_PREFIX$sykmeldingId"

    private fun buildCorrelationKey(correlationId: String) = "$CORRELATION_KEY_PREFIX$correlationId"
}

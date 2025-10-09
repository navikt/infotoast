package no.nav.infotoast.infotrygd.dlq

import java.time.LocalDateTime
import no.nav.infotoast.infotrygd.InfotrygdProcessingState
import no.nav.infotoast.infotrygd.ProcessingStep
import no.nav.infotoast.utils.logger
import no.nav.infotoast.utils.teamLogger
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

/** Data class representing a message that has permanently failed and moved to DLQ */
data class DeadLetterMessage(
    val sykmeldingId: String,
    val processingState: InfotrygdProcessingState,
    val failureReason: String,
    val failedAt: LocalDateTime = LocalDateTime.now(),
    val totalRetries: Int,
    val lastStep: ProcessingStep
)

/**
 * Service for handling permanently failed Infotrygd messages Messages that exceed max retries are
 * moved to a Dead Letter Queue (DLQ)
 */
@Service
class DeadLetterService(private val redisTemplate: RedisTemplate<String, Any>) {
    private val logger = logger()
    private val teamLogger = teamLogger()

    companion object {
        private const val DLQ_KEY_PREFIX = "infotrygd:dlq:"
        private const val DLQ_LIST_KEY = "infotrygd:dlq:list"
        // Keep DLQ messages for 30 days for manual review
        private const val DLQ_TTL_DAYS = 30L
    }

    /** Move a permanently failed message to the Dead Letter Queue */
    fun moveToDeadLetter(
        sykmeldingId: String,
        processingState: InfotrygdProcessingState,
        failureReason: String
    ) {
        try {
            val deadLetterMessage =
                DeadLetterMessage(
                    sykmeldingId = sykmeldingId,
                    processingState = processingState,
                    failureReason = failureReason,
                    failedAt = LocalDateTime.now(),
                    totalRetries = processingState.retryCount,
                    lastStep = processingState.currentStep
                )

            // Store the dead letter message with a longer TTL
            val key = buildDlqKey(sykmeldingId)
            redisTemplate
                .opsForValue()
                .set(key, deadLetterMessage, java.time.Duration.ofDays(DLQ_TTL_DAYS))

            // Add to the DLQ list for monitoring
            redisTemplate.opsForList().rightPush(DLQ_LIST_KEY, sykmeldingId)

            logger.error(
                "Moved sykmelding $sykmeldingId to Dead Letter Queue. Reason: $failureReason, Retries: ${processingState.retryCount}, Last step: ${processingState.currentStep}"
            )

            teamLogger.error(
                "DLQ: Sykmelding $sykmeldingId permanently failed after ${processingState.retryCount} retries. Reason: $failureReason. State: $processingState"
            )

            // TODO: Send alert to monitoring system (Slack, PagerDuty, etc.)
            // TODO: Create manual oppgave for handling
        } catch (e: Exception) {
            logger.error("Failed to move sykmelding $sykmeldingId to DLQ", e)
        }
    }

    /** Retrieve a message from the Dead Letter Queue */
    fun getDeadLetterMessage(sykmeldingId: String): DeadLetterMessage? {
        val key = buildDlqKey(sykmeldingId)
        return redisTemplate.opsForValue().get(key) as? DeadLetterMessage
    }

    /** Get all sykmelding IDs currently in the DLQ */
    fun getAllDeadLetterIds(): List<String> {
        val size = redisTemplate.opsForList().size(DLQ_LIST_KEY) ?: 0
        return redisTemplate
            .opsForList()
            .range(DLQ_LIST_KEY, 0, size - 1)
            ?.filterIsInstance<String>()
            ?: emptyList()
    }

    /** Get count of messages in DLQ */
    fun getDeadLetterCount(): Long {
        return redisTemplate.opsForList().size(DLQ_LIST_KEY) ?: 0
    }

    /**
     * Manually retry a message from the DLQ This can be used by operators to retry after fixing
     * underlying issues
     */
    fun retryFromDeadLetter(sykmeldingId: String): Boolean {
        val deadLetterMessage = getDeadLetterMessage(sykmeldingId)
        if (deadLetterMessage == null) {
            logger.warn("Cannot retry from DLQ: Message not found for sykmelding $sykmeldingId")
            return false
        }

        logger.info(
            "Manual retry requested from DLQ for sykmelding $sykmeldingId. Will reset retry count and move back to processing."
        )

        // TODO: Implement manual retry logic
        // This would involve resetting the state and triggering reprocessing
        return false
    }

    /** Remove a message from the DLQ (after manual resolution) */
    fun removeFromDeadLetter(sykmeldingId: String) {
        val key = buildDlqKey(sykmeldingId)
        redisTemplate.delete(key)
        redisTemplate.opsForList().remove(DLQ_LIST_KEY, 1, sykmeldingId)
        logger.info("Removed sykmelding $sykmeldingId from DLQ")
    }

    private fun buildDlqKey(sykmeldingId: String) = "$DLQ_KEY_PREFIX$sykmeldingId"
}

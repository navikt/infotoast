package no.nav.infotoast.infotrygd.dlq

import java.time.LocalDateTime
import no.nav.infotoast.InfotrygdService
import no.nav.infotoast.infotrygd.InfotrygdProcessingState
import no.nav.infotoast.infotrygd.InfotrygdProcessingStateService
import no.nav.infotoast.infotrygd.ProcessingStep
import no.nav.infotoast.utils.logger
import no.nav.infotoast.utils.teamLogger
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * Scheduled service that retries failed Infotrygd processing Runs periodically to check for
 * messages that need retry
 */
@Service
class RetryScheduler(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val processingStateService: InfotrygdProcessingStateService,
    private val infotrygdService: InfotrygdService,
    private val deadLetterService: DeadLetterService
) {
    private val logger = logger()
    private val teamLogger = teamLogger()

    /**
     * Runs every 5 minutes to check for failed messages that can be retried Uses exponential
     * backoff: 5min, 10min, 20min
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5 minutes, start after 1 minute
    fun retryFailedMessages() {
        try {
            logger.info("Starting retry scheduler check for failed Infotrygd messages")

            val retryCount = findAndRetryFailedMessages()

            if (retryCount > 0) {
                logger.info("Retry scheduler completed: $retryCount messages retried")
                teamLogger.info("Retry scheduler: Retried $retryCount failed Infotrygd messages")
            }
        } catch (e: Exception) {
            logger.error("Error in retry scheduler", e)
            teamLogger.error("Retry scheduler error", e)
        }
    }

    /** Find failed messages and retry them if eligible Returns the number of messages retried */
    private fun findAndRetryFailedMessages(): Int {
        var retriedCount = 0

        // Scan for all processing state keys
        val keys = scanForProcessingKeys()

        for (key in keys) {
            try {
                val sykmeldingId = key.removePrefix("infotrygd:processing:")
                val state = processingStateService.getState(sykmeldingId)

                if (state == null) {
                    continue
                }

                // Check if message is in a retryable failed state
                if (shouldRetry(state)) {
                    val retried = retryMessage(state)
                    if (retried) {
                        retriedCount++
                    }
                }

                // Check if message should be moved to DLQ
                if (shouldMoveToDeadLetter(state)) {
                    moveToDeadLetter(state)
                }
            } catch (e: Exception) {
                logger.error("Error processing retry for key $key", e)
            }
        }

        return retriedCount
    }

    /** Scan Redis for all processing state keys */
    private fun scanForProcessingKeys(): List<String> {
        val keys = mutableListOf<String>()

        try {
            redisTemplate.execute { connection ->
                val cursor =
                    connection
                        .keyCommands()
                        .scan(
                            org.springframework.data.redis.core.ScanOptions.scanOptions()
                                .match("infotrygd:processing:*")
                                .count(100)
                                .build()
                        )

                cursor.forEach { key -> keys.add(String(key)) }
            }
        } catch (e: Exception) {
            logger.error("Error scanning for processing keys", e)
        }

        return keys
    }

    /** Determine if a message should be retried */
    private fun shouldRetry(state: InfotrygdProcessingState): Boolean {
        // Don't retry if already completed or at max retries
        if (state.currentStep == ProcessingStep.COMPLETED) {
            return false
        }

        if (state.retryCount >= InfotrygdProcessingStateService.MAX_RETRY_COUNT) {
            return false
        }

        // Only retry if in FAILED state or stuck in a processing state for too long
        if (state.currentStep != ProcessingStep.FAILED && !isStuck(state)) {
            return false
        }

        // Check if enough time has passed since last error (exponential backoff)
        val lastError = state.lastErrorAt ?: state.updatedAt
        val minutesSinceError =
            java.time.Duration.between(lastError, LocalDateTime.now()).toMinutes()
        val requiredDelay = calculateRetryDelay(state.retryCount)

        return minutesSinceError >= requiredDelay
    }

    /** Check if a message is stuck (no progress for a long time) */
    private fun isStuck(state: InfotrygdProcessingState): Boolean {
        val minutesSinceUpdate =
            java.time.Duration.between(state.updatedAt, LocalDateTime.now()).toMinutes()

        // If no update in 30 minutes, consider it stuck
        return minutesSinceUpdate >= 30
    }

    /** Calculate retry delay using exponential backoff */
    private fun calculateRetryDelay(retryCount: Int): Long {
        return when (retryCount) {
            0 -> 5 // First retry after 5 minutes
            1 -> 10 // Second retry after 10 minutes
            2 -> 20 // Third retry after 20 minutes
            else -> 30 // Fallback
        }
    }

    /** Attempt to retry a failed message */
    private fun retryMessage(state: InfotrygdProcessingState): Boolean {
        val sykmeldingId = state.sykmeldingId

        logger.info(
            "Retrying sykmelding $sykmeldingId (attempt ${state.retryCount + 1}/${InfotrygdProcessingStateService.MAX_RETRY_COUNT})"
        )
        teamLogger.info(
            "Retrying Infotrygd processing for $sykmeldingId, retry ${state.retryCount + 1}"
        )

        try {
            // Determine which step to retry from based on where it failed
            when {
                state.currentStep == ProcessingStep.FAILED ||
                    state.currentStep == ProcessingStep.INITIATED ||
                    state.currentStep == ProcessingStep.SPORRING_SENT -> {
                    // Retry from the beginning - send sporring
                    retryFromSporring(state)
                }
                state.currentStep == ProcessingStep.SPORRING_RECEIVED ||
                    state.currentStep == ProcessingStep.OPPDATERING_SENT -> {
                    // Retry from oppdatering
                    retryFromOppdatering(state)
                }
                else -> {
                    logger.warn(
                        "Cannot retry sykmelding $sykmeldingId from step ${state.currentStep}"
                    )
                    return false
                }
            }

            return true
        } catch (e: Exception) {
            logger.error("Failed to retry sykmelding $sykmeldingId", e)

            // Update state with error
            val errorMsg = "Retry failed: ${e.message}"
            processingStateService.markFailed(sykmeldingId, errorMsg)

            return false
        }
    }

    /** Retry from sporring step (full retry) */
    private fun retryFromSporring(state: InfotrygdProcessingState) {
        val success =
            processingStateService.markForRetry(state.sykmeldingId, ProcessingStep.INITIATED)
        if (!success) {
            return
        }

        // Send sporring message again
        infotrygdService.retrySporring(state)
    }

    /** Retry from oppdatering step (partial retry) */
    private fun retryFromOppdatering(state: InfotrygdProcessingState) {
        val success =
            processingStateService.markForRetry(
                state.sykmeldingId,
                ProcessingStep.SPORRING_RECEIVED
            )
        if (!success) {
            return
        }

        // Send oppdatering message again
        infotrygdService.sendInfotrygdOppdatering(state.sykmeldingId)
    }

    /** Check if message should be moved to Dead Letter Queue */
    private fun shouldMoveToDeadLetter(state: InfotrygdProcessingState): Boolean {
        // Move to DLQ if max retries exceeded and in FAILED state
        return state.currentStep == ProcessingStep.FAILED &&
            state.retryCount >= InfotrygdProcessingStateService.MAX_RETRY_COUNT
    }

    /** Move a permanently failed message to the Dead Letter Queue */
    private fun moveToDeadLetter(state: InfotrygdProcessingState) {
        val errorMsg = state.errorMessage ?: "Unknown error - max retries exceeded"

        logger.error(
            "Moving sykmelding ${state.sykmeldingId} to DLQ after ${state.retryCount} failed retries"
        )

        deadLetterService.moveToDeadLetter(state.sykmeldingId, state, errorMsg)

        // Clean up the processing state after moving to DLQ
        processingStateService.deleteState(state.sykmeldingId)
    }

    /** Get metrics about retry status (for monitoring) */
    fun getRetryMetrics(): RetryMetrics {
        val keys = scanForProcessingKeys()
        var failedCount = 0
        var pendingRetryCount = 0

        for (key in keys) {
            val sykmeldingId = key.removePrefix("infotrygd:processing:")
            val state = processingStateService.getState(sykmeldingId) ?: continue

            if (state.currentStep == ProcessingStep.FAILED) {
                failedCount++
                if (shouldRetry(state)) {
                    pendingRetryCount++
                }
            }
        }

        return RetryMetrics(
            totalFailed = failedCount,
            pendingRetry = pendingRetryCount,
            inDeadLetter = deadLetterService.getDeadLetterCount()
        )
    }
}

data class RetryMetrics(val totalFailed: Int, val pendingRetry: Int, val inDeadLetter: Long)

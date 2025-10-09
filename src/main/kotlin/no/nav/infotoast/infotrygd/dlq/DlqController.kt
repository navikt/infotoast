package no.nav.infotoast.infotrygd.dlq

import no.nav.infotoast.infotrygd.InfotrygdProcessingStateService
import no.nav.infotoast.infotrygd.ProcessingStep
import no.nav.infotoast.utils.logger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST API for monitoring and managing Infotrygd DLQ Provides endpoints for operations teams to
 * inspect and manage failed messages
 */
@RestController
@RequestMapping("/api/infotrygd/dlq")
class DlqController(
    private val processingStateService: InfotrygdProcessingStateService,
    private val deadLetterService: DeadLetterService,
    private val retryScheduler: RetryScheduler
) {
    private val logger = logger()

    /** Get all messages currently in the Dead Letter Queue */
    @GetMapping
    fun getDeadLetterMessages(): ResponseEntity<List<DeadLetterInfo>> {
        val ids = deadLetterService.getAllDeadLetterIds()
        val messages =
            ids.mapNotNull { id ->
                deadLetterService.getDeadLetterMessage(id)?.let { msg ->
                    DeadLetterInfo(
                        sykmeldingId = msg.sykmeldingId,
                        failureReason = msg.failureReason,
                        failedAt = msg.failedAt,
                        totalRetries = msg.totalRetries,
                        lastStep = msg.lastStep
                    )
                }
            }
        return ResponseEntity.ok(messages)
    }

    /** Get a specific message from the DLQ */
    @GetMapping("/{sykmeldingId}")
    fun getDeadLetterMessage(
        @PathVariable sykmeldingId: String
    ): ResponseEntity<DeadLetterMessage> {
        val message = deadLetterService.getDeadLetterMessage(sykmeldingId)
        return if (message != null) {
            ResponseEntity.ok(message)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /** Get DLQ statistics */
    @GetMapping("/stats")
    fun getDlqStats(): ResponseEntity<DlqStats> {
        val count = deadLetterService.getDeadLetterCount()
        return ResponseEntity.ok(DlqStats(count = count))
    }

    /** Remove a message from the DLQ (after manual resolution) */
    @DeleteMapping("/{sykmeldingId}")
    fun removeFromDlq(@PathVariable sykmeldingId: String): ResponseEntity<Void> {
        logger.info("Manual DLQ removal requested for sykmelding $sykmeldingId")
        deadLetterService.removeFromDeadLetter(sykmeldingId)
        return ResponseEntity.noContent().build()
    }

    /** Get retry metrics */
    @GetMapping("/retry/metrics")
    fun getRetryMetrics(): ResponseEntity<RetryMetrics> {
        val metrics = retryScheduler.getRetryMetrics()
        return ResponseEntity.ok(metrics)
    }

    /** Manually trigger retry scheduler (for testing) */
    @PostMapping("/retry/trigger")
    fun triggerRetry(): ResponseEntity<String> {
        logger.info("Manual retry trigger requested")
        retryScheduler.retryFailedMessages()
        return ResponseEntity.ok("Retry scheduler triggered")
    }

    /** Get processing state for a specific sykmelding */
    @GetMapping("/processing/{sykmeldingId}")
    fun getProcessingState(
        @PathVariable sykmeldingId: String
    ): ResponseEntity<no.nav.infotoast.infotrygd.InfotrygdProcessingState> {
        val state = processingStateService.getState(sykmeldingId)
        return if (state != null) {
            ResponseEntity.ok(state)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}

data class DeadLetterInfo(
    val sykmeldingId: String,
    val failureReason: String,
    val failedAt: java.time.LocalDateTime,
    val totalRetries: Int,
    val lastStep: ProcessingStep
)

data class DlqStats(val count: Long)

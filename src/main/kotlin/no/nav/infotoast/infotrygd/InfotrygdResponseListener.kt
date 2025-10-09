package no.nav.infotoast.infotrygd

import jakarta.annotation.PreDestroy
import jakarta.jms.Connection
import jakarta.jms.MessageListener
import jakarta.jms.Session
import jakarta.jms.TextMessage
import no.nav.infotoast.InfotrygdService
import no.nav.infotoast.infotrygd.dlq.DeadLetterService
import no.nav.infotoast.utils.logger
import no.nav.infotoast.utils.teamLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

data class InfotrygdResponse(
    val correlationId: String,
    val responseXml: String,
    val success: Boolean,
    val errorMessage: String? = null,
)

@Component
class InfotrygdResponseListener(
    private val mqConnection: Connection,
    private val processingStateService: InfotrygdProcessingStateService,
    private val infotrygdService: InfotrygdService,
    private val responseParser: InfotrygdResponseParser,
    private val deadLetterService: DeadLetterService,
    @Value("\${mq.queues.infotrygd.svar}") private val infotrygdSvarQueue: String,
) {
    private val logger = logger()
    private val teamLogger = teamLogger()
    private var session: Session? = null
    private var consumer: jakarta.jms.MessageConsumer? = null

    init {
        startListening()
    }

    private fun startListening() {
        try {
            session = mqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            val queue = session?.createQueue("queue:///$infotrygdSvarQueue?targetClient=1")
            consumer = session?.createConsumer(queue)

            consumer?.messageListener = MessageListener { message ->
                try {
                    when (message) {
                        is TextMessage -> {
                            val correlationId = message.jmsCorrelationID
                            val responseXml = message.text

                            logger.info(
                                "Received response from Infotrygd with correlationId: $correlationId"
                            )
                            teamLogger.info(
                                "Received Infotrygd response for correlationId: $correlationId, length: ${responseXml?.length}"
                            )

                            handleInfotrygdResponse(correlationId, responseXml)
                        }
                        else -> {
                            logger.warn(
                                "Received non-text message from Infotrygd: ${message.jmsType}"
                            )
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error processing Infotrygd response", e)
                    teamLogger.error("Error processing Infotrygd response", e)
                }
            }

            logger.info("Started listening for Infotrygd responses on queue: $infotrygdSvarQueue")
        } catch (e: Exception) {
            logger.error("Failed to start Infotrygd response listener", e)
        }
    }

    private fun handleInfotrygdResponse(correlationId: String?, responseXml: String?) {
        if (correlationId == null || responseXml == null) {
            logger.error("Received Infotrygd response with missing correlationId or responseXml")
            return
        }

        try {
            // Get sykmelding ID from correlation ID
            val sykmeldingId = processingStateService.getSykmeldingIdByCorrelation(correlationId)
            if (sykmeldingId == null) {
                logger.error("Could not find sykmelding ID for correlationId: $correlationId")
                return
            }

            // Get current processing state
            val state = processingStateService.getState(sykmeldingId)
            if (state == null) {
                logger.error("Could not find processing state for sykmelding: $sykmeldingId")
                return
            }

            // We only listen for SPORRING responses
            // Oppdatering is fire-and-forget (no response handling)
            when (state.currentStep) {
                ProcessingStep.SPORRING_SENT -> {
                    handleSporringResponse(sykmeldingId, responseXml)
                }
                else -> {
                    logger.warn(
                        "Received response for sykmelding $sykmeldingId in unexpected state: ${state.currentStep}. We only expect responses for SPORRING_SENT. Oppdatering is fire-and-forget."
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling Infotrygd response for correlationId $correlationId", e)

            // Try to mark as failed if we can get the sykmelding ID
            val sykmeldingId = processingStateService.getSykmeldingIdByCorrelation(correlationId)
            sykmeldingId?.let {
                handleFailure(
                    it,
                    e.message ?: "Unknown error processing response",
                    ProcessingStep.SPORRING_SENT
                )
            }
        }
    }

    private fun handleSporringResponse(sykmeldingId: String, responseXml: String) {
        logger.info("Received sporring response for sykmelding $sykmeldingId")
        teamLogger.info("Sporring response for $sykmeldingId: $responseXml")

        // Parse the XML response using ObjectMapper (same as syfosminfotrygd)
        val parsedResponse = responseParser.parseSporringResponse(responseXml)

        // If we got no data back, treat it as a failure
        if (parsedResponse.identDato == null && parsedResponse.tkNummer == null) {
            val errorMsg = "Empty response from Infotrygd sporring (no identDato or tkNummer)"
            logger.error("Infotrygd sporring returned empty data for sykmelding $sykmeldingId")
            teamLogger.error("Sporring empty response for $sykmeldingId")

            handleFailure(sykmeldingId, errorMsg, ProcessingStep.SPORRING_SENT)
            return
        }

        // Update state to SPORRING_RECEIVED
        processingStateService.updateStep(sykmeldingId, ProcessingStep.SPORRING_RECEIVED)

        // Store extracted data from sporring response (identDato and tkNummer)
        processingStateService.updateWithSporringResponse(
            sykmeldingId,
            parsedResponse.identDato,
            parsedResponse.tkNummer
        )

        logger.info(
            "Successfully processed sporring response for $sykmeldingId: identDato=${parsedResponse.identDato}, tkNummer=${parsedResponse.tkNummer}"
        )

        // Trigger oppdatering (fire-and-forget - no response handling)
        try {
            infotrygdService.sendInfotrygdOppdatering(sykmeldingId)
        } catch (e: Exception) {
            logger.error("Failed to send oppdatering for $sykmeldingId", e)
            handleFailure(
                sykmeldingId,
                "Failed to send oppdatering: ${e.message}",
                ProcessingStep.SPORRING_RECEIVED
            )
        }
    }

    /**
     * Handle failure with retry logic If retries are available, mark for retry. Otherwise, mark as
     * permanently failed.
     */
    private fun handleFailure(
        sykmeldingId: String,
        errorMessage: String,
        failedStep: ProcessingStep
    ) {
        if (processingStateService.canRetry(sykmeldingId)) {
            // Determine which step to retry from
            val retryStep =
                when (failedStep) {
                    ProcessingStep.SPORRING_SENT -> ProcessingStep.INITIATED
                    ProcessingStep.OPPDATERING_SENT -> ProcessingStep.SPORRING_RECEIVED
                    else -> ProcessingStep.INITIATED
                }

            val retried = processingStateService.markForRetry(sykmeldingId, retryStep)
            if (retried) {
                logger.info("Marked sykmelding $sykmeldingId for retry from step $retryStep")
                logger.info("Retry will be attempted by the scheduled retry service")
            } else {
                // Max retries exceeded - move to DLQ
                val state = processingStateService.getState(sykmeldingId)
                if (state != null) {
                    deadLetterService.moveToDeadLetter(
                        sykmeldingId,
                        state,
                        "Failed after max retries: $errorMessage"
                    )
                    processingStateService.deleteState(sykmeldingId)
                } else {
                    processingStateService.markFailed(
                        sykmeldingId,
                        "Failed after max retries: $errorMessage"
                    )
                }
            }
        } else {
            // Max retries already exceeded - move to DLQ
            val state = processingStateService.getState(sykmeldingId)
            if (state != null) {
                deadLetterService.moveToDeadLetter(
                    sykmeldingId,
                    state,
                    "Max retries exceeded: $errorMessage"
                )
                processingStateService.deleteState(sykmeldingId)
            } else {
                processingStateService.markFailed(
                    sykmeldingId,
                    "Max retries exceeded: $errorMessage"
                )
            }
            logger.error("Sykmelding $sykmeldingId moved to DLQ after max retries")
        }
    }

    @PreDestroy
    fun cleanup() {
        try {
            consumer?.close()
            session?.close()
            logger.info("Closed Infotrygd response listener")
        } catch (e: Exception) {
            logger.error("Error closing Infotrygd response listener", e)
        }
    }
}

package no.nav.infotoast.infotrygd

import jakarta.annotation.PreDestroy
import jakarta.jms.Connection
import jakarta.jms.MessageListener
import jakarta.jms.Session
import jakarta.jms.TextMessage
import no.nav.infotoast.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class InfotrygdResponseListener(
    private val mqConnection: Connection,
    @Value("\${mq.queues.infotrygd.svar}") private val infotrygdSvarQueue: String,
) {
    private val logger = logger()
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
                            val response = message.text
                            logger.info("Received response from Infotrygd: $response")
                            handleInfotrygdResponse(response)
                        }
                        else -> {
                            logger.warn(
                                "Received non-text message from Infotrygd: ${message.jmsType}"
                            )
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error processing Infotrygd response", e)
                }
            }

            logger.info("Started listening for Infotrygd responses on queue: $infotrygdSvarQueue")
        } catch (e: Exception) {
            logger.error("Failed to start Infotrygd response listener", e)
        }
    }

    private fun handleInfotrygdResponse(response: String) {
        // TODO: Parse the XML response and update the appropriate sykmelding record
        // This will be implemented when we have the fellesformat structure
        logger.info("Processing Infotrygd response: $response")
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

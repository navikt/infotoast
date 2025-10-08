package no.nav.infotoast.infotrygd.mq

import jakarta.jms.Connection
import jakarta.jms.MessageProducer
import jakarta.jms.Session
import no.nav.infotoast.mq.producerForQueue
import no.nav.infotoast.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class InfotrygdMqService(
    private val mqConnection: Connection,
    @Value("\${mq.queues.infotrygd.oppdatering}") private val infotrygdOppdateringQueue: String,
    @Value("\${mq.queues.infotrygd.svar}") private val infotrygdSvarQueue: String,
    @Value("\${mq.queues.infotrygd.sporring}") private val infotrygdSporringQueue: String,
) {
    private val logger = logger()

    fun sendInfotrygdOppdatering(xmlMessage: String) {
        val session = mqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        try {
            val producer =
                session.producerForQueue("queue:///$infotrygdOppdateringQueue?targetClient=1")
            val textMessage = session.createTextMessage().apply { text = xmlMessage }
            producer.send(textMessage)
            logger.info(
                "Successfully sent Infotrygd oppdatering message to queue: $infotrygdOppdateringQueue"
            )
        } finally {
            session.close()
        }
    }

    fun sendInfotrygdSporring(xmlMessage: String): String {
        val session = mqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        try {
            val producer =
                session.producerForQueue("queue:///$infotrygdSporringQueue?targetClient=1")

            // Create temporary queue for reply
            val replyQueue = session.createTemporaryQueue()
            val consumer = session.createConsumer(replyQueue)

            val textMessage =
                session.createTextMessage().apply {
                    text = xmlMessage
                    jmsReplyTo = replyQueue
                }

            producer.send(textMessage)
            logger.info(
                "Successfully sent Infotrygd sporring message to queue: $infotrygdSporringQueue"
            )

            // Wait for response (with timeout)
            val responseMessage = consumer.receive(30000) // 30 second timeout

            if (responseMessage == null) {
                logger.error("Timeout waiting for response from Infotrygd sporring")
                throw RuntimeException("Timeout waiting for response from Infotrygd")
            }

            val response = (responseMessage as jakarta.jms.TextMessage).text
            logger.info("Received response from Infotrygd sporring")
            return response
        } finally {
            session.close()
        }
    }

    fun createSession(): Session {
        return mqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    }

    fun getOppdateringProducer(session: Session): MessageProducer {
        return session.producerForQueue("queue:///$infotrygdOppdateringQueue?targetClient=1")
    }

    fun getSporringProducer(session: Session): MessageProducer {
        return session.producerForQueue("queue:///$infotrygdSporringQueue?targetClient=1")
    }
}

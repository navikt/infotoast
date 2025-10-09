package no.nav.infotoast.infotrygd.mq

import jakarta.jms.Connection
import jakarta.jms.MessageProducer
import jakarta.jms.Session
import java.util.UUID
import no.nav.infotoast.mq.producerForQueue
import no.nav.infotoast.utils.logger
import no.nav.infotoast.utils.teamLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
class InfotrygdMqService(
    private val mqConnection: Connection,
    @Value("\${mq.queues.infotrygd.oppdatering}") private val infotrygdOppdateringQueue: String,
    @Value("\${mq.queues.infotrygd.svar}") private val infotrygdSvarQueue: String,
    @Value("\${mq.queues.infotrygd.sporring}") private val infotrygdSporringQueue: String,
) : IInfotrygdMqService {
    private val logger = logger()
    private val teamLogger = teamLogger()

    /**
     * Sends an Infotrygd query (sporring/forespørsel) to check existing data Returns the
     * correlation ID for tracking the response
     */
    override fun sendInfotrygdSporring(xmlMessage: String, sykmeldingId: String): String {
        val session = mqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        try {
            val correlationId = UUID.randomUUID().toString()
            val producer =
                session.producerForQueue("queue:///$infotrygdSporringQueue?targetClient=1")

            val textMessage =
                session.createTextMessage().apply {
                    text = xmlMessage
                    jmsCorrelationID = correlationId
                    // TODO: Update queue name when we have the actual static reply queue name
                    jmsReplyTo = session.createQueue(infotrygdSvarQueue)
                }

            producer.send(textMessage)
            logger.info(
                "Sent Infotrygd sporring for sykmelding $sykmeldingId with correlationId $correlationId"
            )
            teamLogger.info("Sent Infotrygd sporring for sykmelding $sykmeldingId")

            return correlationId
        } finally {
            session.close()
        }
    }

    /**
     * Sends an Infotrygd update (oppdatering) message Returns the correlation ID for tracking the
     * response
     */
    override fun sendInfotrygdOppdatering(xmlMessage: String, sykmeldingId: String): String {
        val session = mqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        try {
            val correlationId = UUID.randomUUID().toString()
            val producer =
                session.producerForQueue("queue:///$infotrygdOppdateringQueue?targetClient=1")

            val textMessage =
                session.createTextMessage().apply {
                    text = xmlMessage
                    jmsCorrelationID = correlationId
                    // TODO: Update queue name when we have the actual static reply queue name
                    jmsReplyTo = session.createQueue(infotrygdSvarQueue)
                }

            producer.send(textMessage)
            logger.info(
                "Sent Infotrygd oppdatering for sykmelding $sykmeldingId with correlationId $correlationId"
            )
            teamLogger.info(
                "Sent Infotrygd oppdatering for sykmelding $sykmeldingId, message length: ${xmlMessage.length}"
            )

            return correlationId
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

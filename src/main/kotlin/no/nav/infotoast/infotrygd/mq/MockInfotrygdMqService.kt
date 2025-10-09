package no.nav.infotoast.infotrygd.mq

import java.util.UUID
import no.nav.infotoast.utils.logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("local", "test")
@Service
class MockInfotrygdMqService : IInfotrygdMqService {
    private val logger = logger()

    /** Mock implementation that returns a generated correlation ID without connecting to MQ */
    override fun sendInfotrygdSporring(xmlMessage: String, sykmeldingId: String): String {
        val correlationId = UUID.randomUUID().toString()

        logger.info(
            "MOCK: Would send Infotrygd sporring for sykmelding $sykmeldingId with correlationId $correlationId"
        )
        logger.info("MOCK: Sporring XML message length: ${xmlMessage.length}")

        return correlationId
    }

    /** Mock implementation that returns a generated correlation ID without connecting to MQ */
    override fun sendInfotrygdOppdatering(xmlMessage: String, sykmeldingId: String): String {
        val correlationId = UUID.randomUUID().toString()

        logger.info(
            "MOCK: Would send Infotrygd oppdatering for sykmelding $sykmeldingId with correlationId $correlationId"
        )
        logger.info("MOCK: Oppdatering XML message length: ${xmlMessage.length}")

        return correlationId
    }
}

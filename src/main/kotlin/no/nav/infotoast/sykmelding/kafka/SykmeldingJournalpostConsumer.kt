package no.nav.infotoast.sykmelding.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.infotoast.sykmelding.SykmeldingWithJournalpostIdRecord
import no.nav.infotoast.utils.logger
import no.nav.infotoast.utils.teamLogger
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class SykmeldingJournalpostConsumer(
    @param:Value($$"${nais.cluster}") private val clusterName: String,
) {
    private val logger = logger()
    private val teamLogger = teamLogger()

    @KafkaListener(
        topics = [$$"${kafka.topics.sykmeldinger-journalpost}"],
        groupId = "infotoast-consumer",
        containerFactory = "kafkaListenerContainerFactory",
        batch = "false",
    )
    fun consume(record: ConsumerRecord<String, ByteArray?>) {
        val sykmeldingId = record.key()

        val value: ByteArray? = record.value()

        teamLogger.info("Consuming record (id: $sykmeldingId): $value from topic ${record.topic()}")

        if (value == null) {
            logger.warn("konsumert sykmelding med id $sykmeldingId er null, hopper over")
            return
        }

        //TODO this needs a rewrite becaause we are not reading the sykmeldinger topic anymore, its the sykmeldinger-journalpost topic which has
        // sykmeldinger and journalpostId.
                // keyen må være sykmeldingsId
        try {
            val sykmeldingWithJournalpostIdRecord = sykmeldingObjectMapper.readValue<SykmeldingWithJournalpostIdRecord>(value)
            val sykmeldingRecord = sykmeldingWithJournalpostIdRecord.sykmeldingRecord

            // TODO Treng vi sjekke person og sykmelder? sjekk kva infotrygd gjer. Mulig vi også må slå opp sykmelder og sende vidare til servicen.. smtss oppslaget treng.

            // TODO kall service som interacter med infotrygd med forespørsel.

        } catch (e: Exception) {
            logger.error(
                "Kafka consumer failed, key: ${record.key()} - Error processing record",
                e,
            )
            teamLogger.error(
                "Kafka consumer failed, key: ${record.key()} - Error processing record, data: $value",
                e,
            )

            // Don't eat the exception, we don't want to commit on unexpected errors
            throw e
        }
    }
}

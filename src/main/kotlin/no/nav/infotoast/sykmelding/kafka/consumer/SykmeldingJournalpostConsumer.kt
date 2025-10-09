package no.nav.infotoast.sykmelding.kafka.consumer

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.infotoast.sykmelding.MottattSykmeldingService
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
    private val mottattSykmeldingService: MottattSykmeldingService,
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

        try {
            val sykmeldingWithJournalpostIdRecord =
                sykmeldingObjectMapper.readValue<SykmeldingWithJournalpostIdRecord>(value)

            Thread.sleep(5000)
            println(
                "we managed to read a sykmelding with id ${sykmeldingId} from topic and journalpostId ${sykmeldingWithJournalpostIdRecord.journalpostId}"
            )
            //            mottattSykmeldingService.handleMessage(
            //                sykmeldingId = sykmeldingId,
            //                sykmeldingRecord = sykmeldingWithJournalpostIdRecord.sykmeldingRecord,
            //                journalpostId = sykmeldingWithJournalpostIdRecord.journalpostId,
            //            )
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

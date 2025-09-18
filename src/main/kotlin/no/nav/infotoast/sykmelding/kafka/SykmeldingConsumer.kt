package no.nav.infotoast.sykmelding.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.infotoast.utils.logger
import no.nav.infotoast.utils.teamLogger
import no.nav.tsm.syk_inn_api.sykmelding.isBeforeYear
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class SykmeldingConsumer(
    @param:Value($$"${nais.cluster}") private val clusterName: String,
) {
    private val logger = logger()
    private val teamLogger = teamLogger()

    @KafkaListener(
        topics = [$$"${kafka.topics.sykmeldinger}"],
        groupId = "syk-inn-api-consumer",
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
            val sykmeldingRecord = sykmeldingObjectMapper.readValue<SykmeldingRecord>(value)
            if (sykmeldingRecord.sykmelding.aktivitet.isEmpty()) {
                logger.warn(
                    "SykmeldingRecord with id=${record.key()} has no activity, skipping processing",
                )
                return
            }

            if (sykmeldingRecord.isBeforeYear(2024)) {
                return // Skip processing for sykmeldinger before 2024
            }

            if (
                sykmeldingRecord.sykmelding.type ==
                    no.nav.tsm.sykmelding.input.core.model.SykmeldingType.UTENLANDSK
            ) {
                return // skip processing for utenlandske sykmeldinger
            }

            // TODO Treng vi sjekke person og sykmelder? sjekk kva infotrygd gjer
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

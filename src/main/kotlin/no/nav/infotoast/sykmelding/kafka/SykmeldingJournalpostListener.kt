package no.nav.infotoast.sykmelding.kafka

import no.nav.infotoast.utils.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class SykmeldingJournalpostListener() {

    val logger = logger()

    @KafkaListener(
        topics = ["\${spring.kafka.topics.sykmeldinger-journalpost}"],
        groupId = "infotoast-consumer",
        containerFactory = "containerFactory",
    )
    fun listen(cr: ConsumerRecord<String, ByteArray>) {
        logger.info(
            "sykmeldingRecord from kafka: key=${cr.key()}, offset=${cr.offset()}, partition: ${cr.partition()}"
        )
    }
}

package no.nav.infotoast.sykmelding.kafka.producer

import no.nav.infotoast.oppgave.OppgaveRecord
import no.nav.infotoast.utils.logger
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.stereotype.Component

@Component
class OppgaveProducer(
    private val kafkaProducer: KafkaProducer<String, OppgaveRecord>,
    private val oppgaveTopic: String,
) {

    private val logger = logger()

    fun opprettOppgave(oppgaveRecord: OppgaveRecord) {
        logger.info(
            "Oppretter oppgave med id ${oppgaveRecord.produserOppgave.messageId} ved å sende til topic $oppgaveTopic"
        )
        sendOppgaveToKafka(
            oppgaveRecord = oppgaveRecord,
        )
    }

    private fun sendOppgaveToKafka(
        oppgaveRecord: OppgaveRecord,
    ) {
        val key =
            oppgaveRecord.produserOppgave
                .messageId // TODO er dette rett id å bruke? skulle egentlig ha oppgaveId
        val producerRecord: ProducerRecord<String, OppgaveRecord> =
            ProducerRecord(oppgaveTopic, key, oppgaveRecord)
        kafkaProducer.send(producerRecord).get()
    }

    // TODO check if we can delete
    fun tombstoneOppgave(oppgaveId: String) {
        logger.info("Tombstoner oppgave med id: $oppgaveId på topic $oppgaveTopic")
        val producerRecord: ProducerRecord<String, OppgaveRecord> =
            ProducerRecord(oppgaveTopic, oppgaveId, null)
        kafkaProducer.send(producerRecord).get()
    }
}

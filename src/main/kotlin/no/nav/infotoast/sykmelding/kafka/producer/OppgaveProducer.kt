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
    private val sourceApp: String,
    private val sourceNamespace: String,
) {

    private val logger = logger()

    companion object {
        private const val SOURCE_APP = "source-app"
        private const val SOURCE_NAMESPACE = "source-namespace"
    }

    fun opprettOppgave(oppgaveRecord: OppgaveRecord) {
        logger.info(
            "Oppretter oppgave med id ${oppgaveRecord.oppgaveId} ved å sende til topic $oppgaveTopic"
        )
        sendOppgaveToKafka(
            oppgaveRecord = oppgaveRecord,
            sourceApp = sourceApp,
            sourceNamespace = sourceNamespace,
        )
    }

    private fun sendOppgaveToKafka(
        oppgaveRecord: OppgaveRecord,
        sourceApp: String,
        sourceNamespace: String,
        additionalHeaders: Map<String, String> = emptyMap(),
    ) {
        val key = oppgaveRecord.oppgaveId.toString()
        val producerRecord: ProducerRecord<String, OppgaveRecord> =
            ProducerRecord(oppgaveTopic, key, oppgaveRecord)
        producerRecord.headers().add(SOURCE_APP, sourceApp.toByteArray())
        producerRecord.headers().add(SOURCE_NAMESPACE, sourceNamespace.toByteArray())
        additionalHeaders.forEach { producerRecord.headers().add(it.key, it.value.toByteArray()) }
        kafkaProducer.send(producerRecord).get()
    }

    fun tombstoneOppgave(oppgaveId: String) {
        logger.info("Tombstoner oppgave med id: $oppgaveId på topic $oppgaveTopic")
        tombstoneOppgave(
            oppgaveId = oppgaveId,
            sourceApp = sourceApp,
            sourceNamespace = sourceNamespace,
        )
    }

    private fun tombstoneOppgave(
        oppgaveId: String,
        sourceApp: String,
        sourceNamespace: String,
        additionalHeaders: Map<String, String> = emptyMap(),
    ) {
        val producerRecord: ProducerRecord<String, OppgaveRecord> =
            ProducerRecord(oppgaveTopic, oppgaveId, null)
        producerRecord.headers().add(SOURCE_APP, sourceApp.toByteArray())
        producerRecord.headers().add(SOURCE_NAMESPACE, sourceNamespace.toByteArray())
        additionalHeaders.forEach { producerRecord.headers().add(it.key, it.value.toByteArray()) }
        kafkaProducer.send(producerRecord).get()
    }
}

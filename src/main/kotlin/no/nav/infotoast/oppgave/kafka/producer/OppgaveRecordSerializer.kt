package no.nav.infotoast.oppgave.kafka.producer

import no.nav.infotoast.oppgave.kafka.OppgaveRecord
import org.apache.kafka.common.serialization.Serializer

internal class OppgaveRecordSerializer : Serializer<OppgaveRecord?> {

    override fun serialize(topic: String, oppgaveRecord: OppgaveRecord?): ByteArray? {
        return when (oppgaveRecord) {
            null -> null
            else -> oppgaveObjectMapper.writeValueAsBytes(oppgaveRecord)
        }
    }
}

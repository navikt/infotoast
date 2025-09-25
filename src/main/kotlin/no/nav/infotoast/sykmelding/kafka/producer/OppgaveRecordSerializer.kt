package no.nav.infotoast.sykmelding.kafka.producer

import no.nav.infotoast.oppgave.OppgaveRecord
import org.apache.kafka.common.serialization.Serializer

internal class OppgaveRecordSerializer : Serializer<OppgaveRecord?> {

    override fun serialize(topic: String, oppgaveRecord: OppgaveRecord?): ByteArray? {
        return when (oppgaveRecord) {
            null -> null
            else -> oppgaveObjectMapper.writeValueAsBytes(oppgaveRecord)
        }
    }
}

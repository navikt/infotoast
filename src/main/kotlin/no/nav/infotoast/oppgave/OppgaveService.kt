package no.nav.infotoast.oppgave

import no.nav.infotoast.sykmelding.kafka.producer.OppgaveProducer
import no.nav.infotoast.utils.logger
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.springframework.stereotype.Service

@Service
class OppgaveService(
    private val oppgaveProducer: OppgaveProducer,
) {
    private val logger = logger()

    fun produceOppgave(sykmeldingRecord: SykmeldingRecord) {
        val oppgaveRecord = getOppgaveRecord(sykmeldingRecord)

        oppgaveProducer.opprettOppgave(oppgaveRecord)
        logger.info("Opprettet oppgave for sykmeldingId ${sykmeldingRecord.sykmelding.id}")

    }

    private fun getOppgaveRecord(sykmeldingRecord: SykmeldingRecord): OppgaveRecord {
        //TODO implement
    }
}

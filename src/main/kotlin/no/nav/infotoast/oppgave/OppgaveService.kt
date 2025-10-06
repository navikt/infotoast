package no.nav.infotoast.oppgave

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import no.nav.infotoast.person.Person
import no.nav.infotoast.person.pdl.IDENT_GRUPPE
import no.nav.infotoast.sykmelding.kafka.producer.OppgaveProducer
import no.nav.infotoast.utils.logger
import no.nav.tsm.sykmelding.input.core.model.Behandler
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.Papirsykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.UtenlandskSykmelding
import no.nav.tsm.sykmelding.input.core.model.XmlSykmelding
import org.springframework.stereotype.Service

@Service
class OppgaveService(
    private val oppgaveProducer: OppgaveProducer,
) {
    private val logger = logger()

    fun produceOppgave(sykmeldingRecord: SykmeldingRecord, journalpostId: String, person: Person) {
        val oppgaveRecord = getOppgaveRecord(sykmeldingRecord, journalpostId, person)

        oppgaveProducer.opprettOppgave(oppgaveRecord)
        logger.info("Opprettet oppgave for sykmeldingId ${sykmeldingRecord.sykmelding.id}")
    }

    private fun getOppgaveRecord(sykmeldingRecord: SykmeldingRecord, journalpostId: String, person: Person): OppgaveRecord {
        val sykmeldingId = sykmeldingRecord.sykmelding.id
        val isUtenlandsk = sykmeldingRecord.sykmelding is UtenlandskSykmelding
        val behandlerAktoerId = getAktoerId(person)
        val beskrivelse = "Manuell behandling av sykmelding. Validation status: ${sykmeldingRecord.validation.status}"

        // Calculate frist based on whether it's utenlandsk sykmelding
        val fristDager = if (isUtenlandsk) 1 else 4
        val fristFerdigstillelse = finnFristForFerdigstillingAvOppgave(LocalDate.now().plusDays(fristDager.toLong()))

        return OppgaveRecord(
            produserOppgave = ProduserOppgaveKafkaMessage(
                messageId = sykmeldingId,
                aktoerId = behandlerAktoerId,
                tildeltEnhetsnr = "",
                opprettetAvEnhetsnr = "9999",
                behandlesAvApplikasjon = "FS22", // Gosys
                orgnr = "",
                beskrivelse = beskrivelse,
                temagruppe = "ANY",
                tema = "SYM",
                behandlingstema = "ANY",
                oppgavetype = "BEH_EL_SYM",
                behandlingstype = if (isUtenlandsk) "ae0106" else "ANY",
                mappeId = 1,
                aktivDato = DateTimeFormatter.ISO_DATE.format(LocalDate.now()),
                fristFerdigstillelse = DateTimeFormatter.ISO_DATE.format(fristFerdigstillelse),
                prioritet = PrioritetType.NORM,
                metadata = emptyMap()
            ),
            journalOpprettet = JournalKafkaMessage(
                messageId = sykmeldingId,
                journalpostId = journalpostId,
                journalpostKilde = TODO("")
            )
        )
    }

    private fun getAktoerId(person: Person): String {
        return person.identer
            .find { it.gruppe == IDENT_GRUPPE.AKTORID && !it.historisk }
            ?.ident
            ?: throw IllegalStateException("Kunne ikke finne aktørId for behandler")
    }

    private fun getBehandler(sykmeldingRecord: SykmeldingRecord): Behandler {
        return when (val value = sykmeldingRecord.sykmelding) {
            is DigitalSykmelding -> value.behandler
            is Papirsykmelding -> value.behandler
            is XmlSykmelding -> value.behandler
            is UtenlandskSykmelding -> throw IllegalArgumentException("UtenlandskSykmelding has no behandler")
        }
    }

    private fun finnFristForFerdigstillingAvOppgave(ferdistilleDato: LocalDate): LocalDate {
        return setToWorkDay(ferdistilleDato)
    }

    private fun setToWorkDay(ferdistilleDato: LocalDate): LocalDate =
        when (ferdistilleDato.dayOfWeek) {
            DayOfWeek.SATURDAY -> ferdistilleDato.plusDays(2)
            DayOfWeek.SUNDAY -> ferdistilleDato.plusDays(1)
            else -> ferdistilleDato
        }
}

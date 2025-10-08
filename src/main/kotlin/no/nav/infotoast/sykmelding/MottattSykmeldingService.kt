package no.nav.infotoast.sykmelding

import java.time.LocalDate
import no.nav.infotoast.InfotrygdService
import no.nav.infotoast.oppgave.OppgaveService
import no.nav.infotoast.person.PersonService
import no.nav.infotoast.sykmelder.tss.TssService
import no.nav.infotoast.utils.logger
import no.nav.infotoast.utils.teamLogger
import no.nav.tsm.sykmelding.input.core.model.Behandler
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.Papirsykmelding
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.UtenlandskSykmelding
import no.nav.tsm.sykmelding.input.core.model.XmlSykmelding
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType
import org.springframework.stereotype.Service

@Service
class MottattSykmeldingService(
    private val tssService: TssService,
    private val manuellBehandlingService: OppgaveService,
    private val personService: PersonService,
    private val infotrygdService: InfotrygdService,
) {
    private val logger = logger()
    private val teamLogger = teamLogger()

    fun handleMessage(
        sykmeldingId: String,
        sykmeldingRecord: SykmeldingRecord,
        journalpostId: String
    ) {

        val tssId =
            tssService.getTssId(
                fnr = getSykmelderFnr(sykmeldingRecord),
                orgName = "",
                sykmeldingId = sykmeldingId,
            )

        logger.info("Sykmelding med id $sykmeldingId har tssId $tssId")

        val pdlPerson = personService.getPerson(getSykmelderFnr(sykmeldingRecord))

        if (sykmeldingRecord.validation.status != RuleType.PENDING) {
            // TODO (happy path)
            // Happy path should create a request to infotrygd and send it on the mq
            infotrygdService.updateInfotrygd(
                tssId = tssId,
                sykmeldingRecord = sykmeldingRecord,
                journalpostId = journalpostId,
                pdlPerson = pdlPerson,
            )
        } else {
            logger.info(
                "Sykmelding med id $sykmeldingId har validation result ${sykmeldingRecord.validation.status}, denne kan ikke Infotrygd prosessere automatisk, oppretter oppgave"
            )
            manuellBehandlingService.produceOppgave(sykmeldingRecord, journalpostId, pdlPerson)
        }

        // TODO Treng vi sjekke person og sykmelder? sjekk kva infotrygd gjer. Mulig vi også må slå
        // opp sykmelder og sende vidare til servicen.. smtss oppslaget treng.

    }

    private fun getSykmelderFnr(sykmeldingRecord: SykmeldingRecord): String {
        return when (val value = sykmeldingRecord.sykmelding) {
            is DigitalSykmelding -> {
                findFnr(value.behandler)
            }
            is Papirsykmelding -> {
                findFnr(value.behandler)
            }
            is XmlSykmelding -> {
                findFnr(value.behandler)
            }
            is UtenlandskSykmelding -> {
                "0"
            }
        }
    }

    private fun findFnr(behandler: Behandler): String {
        val fnr = behandler.ids.find { it.type == PersonIdType.FNR }?.id
        requireNotNull(fnr) { "Sykmelding mangler fnr for behandler" }
        return fnr
    }
}

fun List<Aktivitet>.sortedFOMDate(): List<LocalDate> = map { it.fom }.sorted()

fun List<Aktivitet>.sortedTOMDate(): List<LocalDate> = map { it.tom }.sorted()

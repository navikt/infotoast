package no.nav.infotoast.sykmelding

import no.nav.infotoast.InfotrygdService
import no.nav.infotoast.norg.NavKontorService
import no.nav.infotoast.oppgave.OppgaveService
import no.nav.infotoast.person.PersonService
import no.nav.infotoast.sykmelder.hpr.HprService
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
    private val hprService: HprService,
    private val navKontorService: NavKontorService,
) {
    private val logger = logger()
    private val teamLogger = teamLogger()

    fun handleMessage(
        sykmeldingId: String,
        sykmeldingRecord: SykmeldingRecord,
        journalpostId: String
    ) {

        val sykmelderFnr = getSykmelderFnr(sykmeldingRecord)

        val tssId =
            tssService.getTssId(
                fnr = sykmelderFnr,
                orgName = "",
                sykmeldingId = sykmeldingId,
            )

        logger.info("Sykmelding med id $sykmeldingId har tssId $tssId")

        val pdlPerson = personService.getPerson(sykmelderFnr)

        if (sykmeldingRecord.validation.status != RuleType.PENDING) {
            // Get healthcare personnel category from HPR
            val helsepersonellKategori =
                hprService.getHelsepersonellKategori(sykmelderFnr, sykmeldingId)
            logger.info(
                "Sykmelding med id $sykmeldingId har helsepersonellkategori $helsepersonellKategori"
            )

            // Get NAV office number from NORG2 (or 2101 for utenlandsk sykmeldinger)
            val navKontorNr =
                navKontorService.finnLokaltNavkontor(pdlPerson, sykmeldingRecord, sykmeldingId)
            logger.info("Sykmelding med id $sykmeldingId har NAV kontor $navKontorNr")

            // Initiate async Infotrygd processing (sends sporring, response listener will handle
            // oppdatering)
            infotrygdService.initiateInfotrygdProcessing(
                tssId = tssId,
                sykmeldingRecord = sykmeldingRecord,
                journalpostId = journalpostId,
                pdlPerson = pdlPerson,
                helsepersonellKategori = helsepersonellKategori,
                navKontorNr = navKontorNr,
            )
        } else {
            logger.info(
                "Sykmelding med id $sykmeldingId har validation result ${sykmeldingRecord.validation.status}, denne kan ikke Infotrygd prosessere automatisk, oppretter oppgave"
            )
            manuellBehandlingService.produceOppgave(sykmeldingRecord, journalpostId, pdlPerson)
        }
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

package no.nav.infotoast.sykmelding

import java.time.LocalDate
import no.nav.infotoast.oppgave.OppgaveService
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
                orgName = getOrgNameForSykmelder(sykmeldingRecord),
                sykmeldingId = sykmeldingId,
            )

        logger.info("Sykmelding med id $sykmeldingId har tssId $tssId")

        // sjekk om validation result er manual processing

        if (sykmeldingRecord.validation.status != RuleType.PENDING) {
            logger.info(
                "Sykmelding med id $sykmeldingId har validation result ${sykmeldingRecord.validation.status}, denne kan ikke Infotrygd prosessere automatisk, oppretter oppgave"
            )
            manuellBehandlingService.produceOppgave(sykmeldingRecord)
        } else {
            // TODO opprett manuell oppgave - sender ikkje på infotrygd
        }

        // TODO Treng vi sjekke person og sykmelder? sjekk kva infotrygd gjer. Mulig vi også må slå
        // opp sykmelder og sende vidare til servicen.. smtss oppslaget treng.
        // TODO må ha ein data klasse ala receivedSykmelding for å putte ting i, blant anna tss id.
        // denne her må slå opp tssId

    }

    private fun getLegekontorOrgnr(sykmeldingRecord: SykmeldingRecord): String? {
        return "123456789"
        // TODO implement, find a way to find this information for all types of sykmelding
    }

    private fun getOrgNameForSykmelder(sykmeldingRecord: SykmeldingRecord): String {
        return ""
        // TODO we got to figure out what org name to use here - syk-dig-backend sends a blank
        // string.
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

package no.nav.infotoast.infotrygd

import no.nav.infotoast.fellesformat.FellesformatHealthInfo
import no.nav.infotoast.utils.logger
import no.nav.infotoast.utils.teamLogger
import org.springframework.stereotype.Component

@Component
class InfotrygdBlokkBuilder {
    private val logger = logger()
    private val teamLogger = teamLogger()

    /** Builds an InfotrygdBlokk message from all collected data */
    fun buildInfotrygdBlokk(
        healthInfo: FellesformatHealthInfo,
        tssId: String,
        journalpostId: String,
        navKontorNr: String,
        helsepersonellKategori: String,
    ): InfotrygdBlokk {
        logger.info("Building InfotrygdBlokk for sykmelding ${healthInfo.sykmeldingId}")

        // Map diagnoses to Infotrygd format
        val hovedDiagnose =
            healthInfo.hovedDiagnose?.let {
                DiagnoseKode(
                    kode = it.kode,
                    kodeverk = mapDiagnoseKodeverk(it.system),
                    tekst = it.tekst
                )
            }

        val biDiagnoser =
            healthInfo.biDiagnoser.map {
                DiagnoseKode(
                    kode = it.kode,
                    kodeverk = mapDiagnoseKodeverk(it.system),
                    tekst = it.tekst
                )
            }

        // Map periods to Infotrygd format
        val perioder =
            healthInfo.perioder.map { periode ->
                InfotrygdPeriode(
                    fom = periode.fom,
                    tom = periode.tom,
                    grad = periode.grad,
                    typeSykmelding = determineTypeSykmelding(periode)
                )
            }

        // Map employer info
        val arbeidsgiver =
            healthInfo.arbeidsgiver?.let {
                InfotrygdArbeidsgiver(navn = it.navn, orgnummer = it.orgnummer)
            }

        val infotrygdBlokk =
            InfotrygdBlokk(
                sykmeldingId = healthInfo.sykmeldingId,
                pasientFnr = healthInfo.pasientFnr,
                behandlerFnr = healthInfo.behandlerFnr
                        ?: throw IllegalStateException("Behandler FNR is required"),
                behandlerHpr = healthInfo.behandlerHpr,
                helsepersonellKategori = helsepersonellKategori,
                tssId = tssId,
                journalpostId = journalpostId,
                navKontorNr = navKontorNr,
                signaturDato = healthInfo.signaturDato,
                forsteFravaersdag = healthInfo.forsteFravaersdag,
                diagnose =
                    InfotrygdDiagnose(hovedDiagnose = hovedDiagnose, biDiagnoser = biDiagnoser),
                perioder = perioder,
                arbeidsgiver = arbeidsgiver
            )
        teamLogger.info(
            "Built InfotrygdBlokk for sykmelding ${healthInfo.sykmeldingId} with data: $infotrygdBlokk"
        )

        return infotrygdBlokk
    }

    /**
     * Determines the type of sykmelding period based on the period info Returns codes like "1" for
     * aktivitet ikke mulig, "2" for gradert, etc.
     */
    private fun determineTypeSykmelding(
        periode: no.nav.infotoast.fellesformat.PeriodeInfo
    ): String {
        return when {
            periode.aktivitetIkkeMulig -> "1"
            periode.grad != null -> "2"
            periode.behandlingsdager != null -> "3"
            periode.reisetilskudd -> "4"
            periode.avventende != null -> "5"
            else -> "1" // Default to full sick leave
        }
    }
}

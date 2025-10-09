package no.nav.infotoast.fellesformat

import java.time.LocalDate
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.infotoast.gcp.get
import no.nav.infotoast.utils.logger
import org.springframework.stereotype.Service

@Service
class FellesformatExtractorService {
    private val logger = logger()

    /** Extracts all health information from fellesformat for Infotrygd processing */
    fun extractHealthInfo(fellesformat: XMLEIFellesformat): FellesformatHealthInfo {
        val msgHead = fellesformat.get<XMLMsgHead>()
        val healthInfo = fellesformat.get<HelseOpplysningerArbeidsuforhet>()

        val sykmeldingId = extractSykmeldingId(msgHead)
        logger.info("Extracting health info for sykmelding: $sykmeldingId")

        return FellesformatHealthInfo(
            sykmeldingId = sykmeldingId,
            pasientFnr = extractPasientFnr(healthInfo),
            behandlerFnr = extractBehandlerFnr(healthInfo),
            behandlerHpr = extractBehandlerHpr(healthInfo),
            signaturDato = extractSignaturDato(healthInfo),
            hovedDiagnose = extractHovedDiagnose(healthInfo),
            biDiagnoser = extractBiDiagnoser(healthInfo),
            perioder = extractPerioder(healthInfo),
            forsteFravaersdag = extractForsteFravaersdag(healthInfo),
            arbeidsgiver = extractArbeidsgiver(healthInfo),
            meldingTilNav = extractMeldingTilNav(healthInfo),
            kontaktMedPasient = extractKontaktMedPasient(healthInfo)
        )
    }

    private fun extractSykmeldingId(msgHead: XMLMsgHead): String {
        return msgHead.msgInfo.msgId
    }

    private fun extractPasientFnr(healthInfo: HelseOpplysningerArbeidsuforhet): String {
        return healthInfo.pasient.fodselsnummer.id
    }

    private fun extractBehandlerFnr(healthInfo: HelseOpplysningerArbeidsuforhet): String? {
        return healthInfo.behandler.id.firstOrNull { it.typeId.v == "FNR" }?.id
    }

    private fun extractBehandlerHpr(healthInfo: HelseOpplysningerArbeidsuforhet): String? {
        return healthInfo.behandler.id.firstOrNull { it.typeId.v == "HPR" }?.id
    }

    private fun extractSignaturDato(
        healthInfo: HelseOpplysningerArbeidsuforhet
    ): java.time.LocalDateTime {
        return healthInfo.kontaktMedPasient.behandletDato
    }

    private fun extractHovedDiagnose(healthInfo: HelseOpplysningerArbeidsuforhet): DiagnoseInfo? {
        val diagnose = healthInfo.medisinskVurdering?.hovedDiagnose ?: return null

        return DiagnoseInfo(
            kode = diagnose.diagnosekode.v,
            system = diagnose.diagnosekode.s,
            tekst = diagnose.diagnosekode.dn
        )
    }

    private fun extractBiDiagnoser(
        healthInfo: HelseOpplysningerArbeidsuforhet
    ): List<DiagnoseInfo> {
        return healthInfo.medisinskVurdering?.biDiagnoser?.diagnosekode?.map { diagnose ->
            DiagnoseInfo(kode = diagnose.v, system = diagnose.s, tekst = diagnose.dn)
        }
            ?: emptyList()
    }

    private fun extractPerioder(healthInfo: HelseOpplysningerArbeidsuforhet): List<PeriodeInfo> {
        return healthInfo.aktivitet.periode.map { periode ->
            PeriodeInfo(
                fom = periode.periodeFOMDato,
                tom = periode.periodeTOMDato,
                grad = periode.gradertSykmelding?.sykmeldingsgrad,
                aktivitetIkkeMulig = periode.aktivitetIkkeMulig != null,
                behandlingsdager = periode.behandlingsdager?.antallBehandlingsdagerUke,
                reisetilskudd = periode.isReisetilskudd == true,
                avventende = periode.avventendeSykmelding?.innspillTilArbeidsgiver
            )
        }
    }

    private fun extractForsteFravaersdag(healthInfo: HelseOpplysningerArbeidsuforhet): LocalDate? {
        // First sick day is the earliest start date of all periods
        return healthInfo.aktivitet.periode.minOfOrNull { it.periodeFOMDato }
    }

    private fun extractArbeidsgiver(
        healthInfo: HelseOpplysningerArbeidsuforhet
    ): ArbeidsgiverInfo? {
        val arbeidsgiver = healthInfo.arbeidsgiver ?: return null

        return ArbeidsgiverInfo(
            navn = arbeidsgiver.navnArbeidsgiver,
            orgnummer = arbeidsgiver.yrkesbetegnelse, // Note: This might not be the right field
            stillingsprosent = arbeidsgiver.stillingsprosent
        )
    }

    private fun extractMeldingTilNav(
        healthInfo: HelseOpplysningerArbeidsuforhet
    ): MeldingTilNavInfo? {
        val melding = healthInfo.meldingTilNav ?: return null

        return MeldingTilNavInfo(
            bistandUmiddelbart = melding.isBistandNAVUmiddelbart == true,
            beskrivBistand = melding.beskrivBistandNAV
        )
    }

    private fun extractKontaktMedPasient(
        healthInfo: HelseOpplysningerArbeidsuforhet
    ): KontaktMedPasientInfo {
        val kontakt = healthInfo.kontaktMedPasient

        return KontaktMedPasientInfo(
            kontaktDato = kontakt.behandletDato.toLocalDate(),
            begrunnelseIkkeKontakt = kontakt.begrunnIkkeKontakt
        )
    }
}

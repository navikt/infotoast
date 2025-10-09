package no.nav.infotoast.fellesformat

import java.time.LocalDate
import java.time.LocalDateTime

/** Extracted health information from fellesformat for Infotrygd processing */
data class FellesformatHealthInfo(
    val sykmeldingId: String,
    val pasientFnr: String,
    val behandlerFnr: String?,
    val behandlerHpr: String?,
    val signaturDato: LocalDateTime,
    val hovedDiagnose: DiagnoseInfo?,
    val biDiagnoser: List<DiagnoseInfo>,
    val perioder: List<PeriodeInfo>,
    val forsteFravaersdag: LocalDate?,
    val arbeidsgiver: ArbeidsgiverInfo?,
    val meldingTilNav: MeldingTilNavInfo?,
    val kontaktMedPasient: KontaktMedPasientInfo?,
)

data class DiagnoseInfo(
    val kode: String,
    val system: String, // ICD-10, ICPC-2
    val tekst: String?
)

data class PeriodeInfo(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Int?, // Gradert sykmelding percentage
    val aktivitetIkkeMulig: Boolean,
    val behandlingsdager: Int?,
    val reisetilskudd: Boolean,
    val avventende: String?
)

data class ArbeidsgiverInfo(val navn: String?, val orgnummer: String?, val stillingsprosent: Int?)

data class MeldingTilNavInfo(val bistandUmiddelbart: Boolean, val beskrivBistand: String?)

data class KontaktMedPasientInfo(val kontaktDato: LocalDate?, val begrunnelseIkkeKontakt: String?)

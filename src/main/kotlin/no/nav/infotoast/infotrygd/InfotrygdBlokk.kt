package no.nav.infotoast.infotrygd

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * InfotrygdBlokk represents the complete message structure that Infotrygd expects for sykmelding
 * updates
 */
data class InfotrygdBlokk(
    val sykmeldingId: String,
    val pasientFnr: String,
    val behandlerFnr: String,
    val behandlerHpr: String?,
    val helsepersonellKategori: String,
    val tssId: String,
    val journalpostId: String,
    val navKontorNr: String,
    val signaturDato: LocalDateTime,
    val forsteFravaersdag: LocalDate?,
    val diagnose: InfotrygdDiagnose,
    val perioder: List<InfotrygdPeriode>,
    val arbeidsgiver: InfotrygdArbeidsgiver?,
)

data class InfotrygdDiagnose(
    val hovedDiagnose: DiagnoseKode?,
    val biDiagnoser: List<DiagnoseKode>,
)

data class DiagnoseKode(
    val kode: String,
    val kodeverk: String, // Converted to Infotrygd format (1=ICD10, 2=ICPC2)
    val tekst: String?,
)

data class InfotrygdPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Int?, // Percentage for gradert sykmelding (0-100)
    val typeSykmelding: String, // Type of sick leave period
)

data class InfotrygdArbeidsgiver(
    val navn: String?,
    val orgnummer: String?,
)

/** Maps diagnosis code system from KITH format to Infotrygd format */
fun mapDiagnoseKodeverk(kithSystem: String): String {
    return when (kithSystem) {
        "2.16.578.1.12.4.1.1.7110" -> "1" // ICD-10
        "2.16.578.1.12.4.1.1.7170" -> "2" // ICPC-2
        else -> "1" // Default to ICD-10
    }
}

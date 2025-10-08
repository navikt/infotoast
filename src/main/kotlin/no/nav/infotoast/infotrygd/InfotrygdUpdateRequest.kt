package no.nav.infotoast.infotrygd

import java.time.LocalDate

data class InfotrygdUpdateRequest(
    val sykmeldingId: String,
    val patientFnr: String,
    val doctorFnr: String?,
    val tssId: String?,
    val journalpostId: String,
    val navKontorNr: String,
    val signaturDato: LocalDate,
    val forsteFravaersDag: LocalDate?,
    val hovedDiagnoseKode: String?,
    val hovedDiagnoseKodeverk: String?,
    val biDiagnoseKode: String?,
    val biDiagnoseKodeverk: String?,
    val perioder: List<Periode>
)

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
    val gradert: Int?,
    val aktivitet: String?
)

data class InfotrygdUpdateResponse(val success: Boolean, val errorMessage: String?)

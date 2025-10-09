package no.nav.infotoast.infotrygd

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.infotoast.fellesformat.DiagnoseInfo
import no.nav.infotoast.fellesformat.FellesformatHealthInfo
import no.nav.infotoast.fellesformat.PeriodeInfo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for InfotrygdBlokkBuilder
 *
 * Tests the building of InfotrygdBlokk messages from extracted health information
 */
class InfotrygdBlokkBuilderTest {

    private lateinit var builder: InfotrygdBlokkBuilder

    @BeforeEach
    fun setup() {
        builder = InfotrygdBlokkBuilder()
    }

    @Test
    fun `should build InfotrygdBlokk with complete health information`() {
        // Given
        val healthInfo =
            FellesformatHealthInfo(
                sykmeldingId = "test-123",
                pasientFnr = "12345678901",
                behandlerFnr = "98765432109",
                behandlerHpr = "1234567",
                signaturDato = LocalDateTime.of(2025, 1, 15, 10, 0),
                hovedDiagnose =
                    DiagnoseInfo(kode = "L87", system = "2.16.578.1.12.4.1.1.7170", tekst = "Acne"),
                biDiagnoser =
                    listOf(
                        DiagnoseInfo(
                            kode = "L88",
                            system = "2.16.578.1.12.4.1.1.7170",
                            tekst = "Other"
                        )
                    ),
                perioder =
                    listOf(
                        PeriodeInfo(
                            fom = LocalDate.of(2025, 1, 15),
                            tom = LocalDate.of(2025, 1, 30),
                            grad = 100,
                            aktivitetIkkeMulig = true,
                            behandlingsdager = null,
                            reisetilskudd = false,
                            avventende = null
                        )
                    ),
                forsteFravaersdag = LocalDate.of(2025, 1, 15),
                arbeidsgiver = null,
                meldingTilNav = null,
                kontaktMedPasient = null
            )
        val tssId = "12345"
        val journalpostId = "journal-123"
        val navKontorNr = "0315"
        val helsepersonellKategori = "LE"

        // When
        val result =
            builder.buildInfotrygdBlokk(
                healthInfo = healthInfo,
                tssId = tssId,
                journalpostId = journalpostId,
                navKontorNr = navKontorNr,
                helsepersonellKategori = helsepersonellKategori
            )

        // Then
        assertNotNull(result)
        assertEquals("12345678901", result.pasientFnr)
        assertEquals("98765432109", result.behandlerFnr)
        assertEquals("1234567", result.behandlerHpr)
        assertEquals(tssId, result.tssId)
        assertEquals(journalpostId, result.journalpostId)
        assertEquals(navKontorNr, result.navKontorNr)
        assertEquals(helsepersonellKategori, result.helsepersonellKategori)
        assertEquals("L87", result.diagnose.hovedDiagnose?.kode)
        assertEquals(1, result.perioder.size)
    }

    @Test
    fun `should build InfotrygdBlokk without biDiagnoser`() {
        // Given
        val healthInfo =
            FellesformatHealthInfo(
                sykmeldingId = "test-123",
                pasientFnr = "12345678901",
                behandlerFnr = "98765432109",
                behandlerHpr = "1234567",
                signaturDato = LocalDateTime.of(2025, 1, 15, 10, 0),
                hovedDiagnose =
                    DiagnoseInfo(kode = "L87", system = "2.16.578.1.12.4.1.1.7170", tekst = "Acne"),
                biDiagnoser = emptyList(),
                perioder =
                    listOf(
                        PeriodeInfo(
                            fom = LocalDate.of(2025, 1, 15),
                            tom = LocalDate.of(2025, 1, 30),
                            grad = 100,
                            aktivitetIkkeMulig = true,
                            behandlingsdager = null,
                            reisetilskudd = false,
                            avventende = null
                        )
                    ),
                forsteFravaersdag = LocalDate.of(2025, 1, 15),
                arbeidsgiver = null,
                meldingTilNav = null,
                kontaktMedPasient = null
            )

        // When
        val result =
            builder.buildInfotrygdBlokk(
                healthInfo = healthInfo,
                tssId = "12345",
                journalpostId = "journal-123",
                navKontorNr = "0315",
                helsepersonellKategori = "LE"
            )

        // Then
        assertNotNull(result)
        assertEquals("L87", result.diagnose.hovedDiagnose?.kode)
    }

    @Test
    fun `should handle multiple periods`() {
        // Given
        val healthInfo =
            FellesformatHealthInfo(
                sykmeldingId = "test-123",
                pasientFnr = "12345678901",
                behandlerFnr = "98765432109",
                behandlerHpr = "1234567",
                signaturDato = LocalDateTime.of(2025, 1, 15, 10, 0),
                hovedDiagnose =
                    DiagnoseInfo(kode = "L87", system = "2.16.578.1.12.4.1.1.7170", tekst = "Acne"),
                biDiagnoser = emptyList(),
                perioder =
                    listOf(
                        PeriodeInfo(
                            fom = LocalDate.of(2025, 1, 15),
                            tom = LocalDate.of(2025, 1, 20),
                            grad = 100,
                            aktivitetIkkeMulig = true,
                            behandlingsdager = null,
                            reisetilskudd = false,
                            avventende = null
                        ),
                        PeriodeInfo(
                            fom = LocalDate.of(2025, 1, 21),
                            tom = LocalDate.of(2025, 1, 30),
                            grad = 50,
                            aktivitetIkkeMulig = false,
                            behandlingsdager = null,
                            reisetilskudd = false,
                            avventende = null
                        )
                    ),
                forsteFravaersdag = LocalDate.of(2025, 1, 15),
                arbeidsgiver = null,
                meldingTilNav = null,
                kontaktMedPasient = null
            )

        // When
        val result =
            builder.buildInfotrygdBlokk(
                healthInfo = healthInfo,
                tssId = "12345",
                journalpostId = "journal-123",
                navKontorNr = "0315",
                helsepersonellKategori = "LE"
            )

        // Then
        assertNotNull(result)
        assertEquals(2, result.perioder.size)
    }

    @Test
    fun `should handle different diagnosis code systems`() {
        // Given - ICD-10 diagnosis
        val healthInfoICD10 =
            FellesformatHealthInfo(
                sykmeldingId = "test-123",
                pasientFnr = "12345678901",
                behandlerFnr = "98765432109",
                behandlerHpr = "1234567",
                signaturDato = LocalDateTime.of(2025, 1, 15, 10, 0),
                hovedDiagnose =
                    DiagnoseInfo(
                        kode = "J06.9",
                        system = "2.16.578.1.12.4.1.1.7110", // ICD-10
                        tekst = "Upper respiratory infection"
                    ),
                biDiagnoser = emptyList(),
                perioder =
                    listOf(
                        PeriodeInfo(
                            fom = LocalDate.of(2025, 1, 15),
                            tom = LocalDate.of(2025, 1, 30),
                            grad = 100,
                            aktivitetIkkeMulig = true,
                            behandlingsdager = null,
                            reisetilskudd = false,
                            avventende = null
                        )
                    ),
                forsteFravaersdag = LocalDate.of(2025, 1, 15),
                arbeidsgiver = null,
                meldingTilNav = null,
                kontaktMedPasient = null
            )

        // When
        val result =
            builder.buildInfotrygdBlokk(
                healthInfo = healthInfoICD10,
                tssId = "12345",
                journalpostId = "journal-123",
                navKontorNr = "0315",
                helsepersonellKategori = "LE"
            )

        // Then
        assertNotNull(result)
        assertEquals("J06.9", result.diagnose.hovedDiagnose?.kode)
        // The kodeverk should be mapped from ICD-10 system
        assertNotNull(result.diagnose.hovedDiagnose?.kodeverk)
    }
}

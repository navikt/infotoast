package no.nav.infotoast

import io.mockk.*
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.infotoast.fellesformat.DiagnoseInfo
import no.nav.infotoast.fellesformat.FellesformatExtractorService
import no.nav.infotoast.fellesformat.FellesformatHealthInfo
import no.nav.infotoast.fellesformat.PeriodeInfo
import no.nav.infotoast.gcp.IGcpBucketService
import no.nav.infotoast.infotrygd.*
import no.nav.infotoast.infotrygd.mq.IInfotrygdMqService
import no.nav.infotoast.person.Person
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for InfotrygdService using mocked dependencies
 *
 * These tests verify the async Infotrygd processing flow without requiring actual MQ or external
 * service connections.
 */
class InfotrygdServiceTest {

    private lateinit var infotrygdMqService: IInfotrygdMqService
    private lateinit var gcpBucketService: IGcpBucketService
    private lateinit var fellesformatExtractor: FellesformatExtractorService
    private lateinit var infotrygdBlokkBuilder: InfotrygdBlokkBuilder
    private lateinit var infotrygdXmlBuilder: InfotrygdXmlBuilder
    private lateinit var infotrygdSporringBuilder: InfotrygdSporringBuilder
    private lateinit var processingStateService: InfotrygdProcessingStateService

    private lateinit var infotrygdService: InfotrygdService

    @BeforeEach
    fun setup() {
        infotrygdMqService = mockk()
        gcpBucketService = mockk()
        fellesformatExtractor = mockk()
        infotrygdBlokkBuilder = mockk()
        infotrygdXmlBuilder = mockk()
        infotrygdSporringBuilder = mockk()
        processingStateService = mockk()

        infotrygdService =
            InfotrygdService(
                infotrygdMqService = infotrygdMqService,
                gcpBucketService = gcpBucketService,
                fellesformatExtractor = fellesformatExtractor,
                infotrygdBlokkBuilder = infotrygdBlokkBuilder,
                infotrygdXmlBuilder = infotrygdXmlBuilder,
                infotrygdSporringBuilder = infotrygdSporringBuilder,
                processingStateService = processingStateService
            )
    }

    @Test
    fun `should successfully initiate Infotrygd processing`() {
        // Given
        val sykmeldingId = "test-sykmelding-123"
        val sykmeldingRecord =
            mockk<SykmeldingRecord> { every { sykmelding.id } returns sykmeldingId }
        val tssId = "12345"
        val journalpostId = "journal-123"
        val pdlPerson = mockk<Person>()
        val helsepersonellKategori = "LE"
        val navKontorNr = "0315"

        val fellesformatXml = mockk<XMLEIFellesformat>()
        val healthInfo =
            FellesformatHealthInfo(
                sykmeldingId = sykmeldingId,
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
        val sporringXml = "<sporring>test</sporring>"
        val correlationId = "correlation-123"

        every { gcpBucketService.getFellesformat(sykmeldingId) } returns fellesformatXml
        every { fellesformatExtractor.extractHealthInfo(fellesformatXml) } returns healthInfo
        every { processingStateService.saveState(any()) } just Runs
        every {
            infotrygdSporringBuilder.buildSporringMessage(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns sporringXml
        every { infotrygdMqService.sendInfotrygdSporring(sporringXml, sykmeldingId) } returns
            correlationId
        every { processingStateService.updateStep(any(), any(), any()) } just Runs

        // When
        infotrygdService.initiateInfotrygdProcessing(
            tssId = tssId,
            sykmeldingRecord = sykmeldingRecord,
            journalpostId = journalpostId,
            pdlPerson = pdlPerson,
            helsepersonellKategori = helsepersonellKategori,
            navKontorNr = navKontorNr
        )

        // Then
        verify(exactly = 1) { gcpBucketService.getFellesformat(sykmeldingId) }
        verify(exactly = 1) { fellesformatExtractor.extractHealthInfo(fellesformatXml) }
        verify(exactly = 1) {
            processingStateService.saveState(
                match { state ->
                    state.sykmeldingId == sykmeldingId &&
                        state.journalpostId == journalpostId &&
                        state.tssId == tssId &&
                        state.currentStep == ProcessingStep.INITIATED
                }
            )
        }
        verify(exactly = 1) { infotrygdMqService.sendInfotrygdSporring(sporringXml, sykmeldingId) }
        verify(exactly = 1) {
            processingStateService.updateStep(
                sykmeldingId = sykmeldingId,
                step = ProcessingStep.SPORRING_SENT,
                correlationId = correlationId
            )
        }
    }

    @Test
    fun `should throw exception when fellesformat is not found`() {
        // Given
        val sykmeldingId = "test-sykmelding-123"
        val sykmeldingRecord =
            mockk<SykmeldingRecord> { every { sykmelding.id } returns sykmeldingId }

        every { gcpBucketService.getFellesformat(sykmeldingId) } returns null
        every { processingStateService.saveState(any()) } just Runs
        every { processingStateService.markFailed(any(), any()) } just Runs

        // When & Then
        assertThrows<IllegalStateException> {
            infotrygdService.initiateInfotrygdProcessing(
                tssId = "12345",
                sykmeldingRecord = sykmeldingRecord,
                journalpostId = "journal-123",
                pdlPerson = mockk(),
                helsepersonellKategori = "LE",
                navKontorNr = "0315"
            )
        }

        verify(exactly = 1) { processingStateService.markFailed(sykmeldingId, any()) }
    }

    @Test
    fun `should throw exception when behandlerFnr is missing`() {
        // Given
        val sykmeldingId = "test-sykmelding-123"
        val sykmeldingRecord =
            mockk<SykmeldingRecord> { every { sykmelding.id } returns sykmeldingId }

        val fellesformatXml = mockk<XMLEIFellesformat>()
        val healthInfo =
            mockk<FellesformatHealthInfo>(relaxed = true) {
                every { pasientFnr } returns "12345678901"
                every { behandlerFnr } returns null // Missing behandler FNR
            }

        every { gcpBucketService.getFellesformat(sykmeldingId) } returns fellesformatXml
        every { fellesformatExtractor.extractHealthInfo(fellesformatXml) } returns healthInfo
        every { processingStateService.saveState(any()) } just Runs
        every { processingStateService.markFailed(any(), any()) } just Runs

        // When & Then
        assertThrows<IllegalStateException> {
            infotrygdService.initiateInfotrygdProcessing(
                tssId = "12345",
                sykmeldingRecord = sykmeldingRecord,
                journalpostId = "journal-123",
                pdlPerson = mockk(),
                helsepersonellKategori = "LE",
                navKontorNr = "0315"
            )
        }

        verify(exactly = 1) { processingStateService.markFailed(sykmeldingId, any()) }
    }

    @Test
    fun `should successfully send oppdatering after sporring response`() {
        // Given
        val sykmeldingId = "test-sykmelding-123"
        val state =
            mockk<InfotrygdProcessingState> {
                every { this@mockk.sykmeldingId } returns sykmeldingId
                every { currentStep } returns ProcessingStep.SPORRING_RECEIVED
                every { tssId } returns "12345"
                every { journalpostId } returns "journal-123"
                every { navKontorNr } returns "0315"
                every { helsepersonellKategori } returns "LE"
            }

        val fellesformatXml = mockk<XMLEIFellesformat>()
        val healthInfo = mockk<FellesformatHealthInfo>()
        val infotrygdBlokk = mockk<InfotrygdBlokk>()
        val oppdateringXml = "<oppdatering>test</oppdatering>"
        val correlationId = "correlation-456"

        every { processingStateService.getState(sykmeldingId) } returns state
        every { gcpBucketService.getFellesformat(sykmeldingId) } returns fellesformatXml
        every { fellesformatExtractor.extractHealthInfo(fellesformatXml) } returns healthInfo
        every {
            infotrygdBlokkBuilder.buildInfotrygdBlokk(any(), any(), any(), any(), any())
        } returns infotrygdBlokk
        every { infotrygdXmlBuilder.buildXmlMessage(infotrygdBlokk) } returns oppdateringXml
        every { processingStateService.updateStep(any(), any(), any()) } just Runs
        every { infotrygdMqService.sendInfotrygdOppdatering(oppdateringXml, sykmeldingId) } returns
            correlationId

        // When
        infotrygdService.sendInfotrygdOppdatering(sykmeldingId)

        // Then
        verify(exactly = 1) { processingStateService.getState(sykmeldingId) }
        verify(exactly = 1) {
            infotrygdMqService.sendInfotrygdOppdatering(oppdateringXml, sykmeldingId)
        }
        verify(exactly = 1) {
            processingStateService.updateStep(sykmeldingId, ProcessingStep.OPPDATERING_SENT)
        }
        verify(exactly = 1) {
            processingStateService.updateStep(sykmeldingId, ProcessingStep.COMPLETED)
        }
    }

    @Test
    fun `should throw exception when sending oppdatering in wrong state`() {
        // Given
        val sykmeldingId = "test-sykmelding-123"
        val state =
            mockk<InfotrygdProcessingState> {
                every { this@mockk.sykmeldingId } returns sykmeldingId
                every { currentStep } returns ProcessingStep.INITIATED // Wrong state
            }

        every { processingStateService.getState(sykmeldingId) } returns state
        every { processingStateService.markFailed(any(), any()) } just Runs

        // When & Then
        assertThrows<IllegalStateException> {
            infotrygdService.sendInfotrygdOppdatering(sykmeldingId)
        }

        verify(exactly = 1) { processingStateService.markFailed(sykmeldingId, any()) }
    }

    @Test
    fun `should successfully retry sporring`() {
        // Given
        val sykmeldingId = "test-sykmelding-123"
        val state =
            mockk<InfotrygdProcessingState> {
                every { this@mockk.sykmeldingId } returns sykmeldingId
                every { pasientFnr } returns "12345678901"
                every { behandlerFnr } returns "98765432109"
                every { tssId } returns "12345"
                every { retryCount } returns 1
            }

        val fellesformatXml = mockk<XMLEIFellesformat>()
        val healthInfo =
            mockk<FellesformatHealthInfo> {
                every { hovedDiagnose } returns
                    DiagnoseInfo(kode = "L87", system = "2.16.578.1.12.4.1.1.7170", tekst = "Acne")
                every { biDiagnoser } returns emptyList()
            }
        val sporringXml = "<sporring>retry</sporring>"
        val correlationId = "correlation-retry-123"

        every { gcpBucketService.getFellesformat(sykmeldingId) } returns fellesformatXml
        every { fellesformatExtractor.extractHealthInfo(fellesformatXml) } returns healthInfo
        every {
            infotrygdSporringBuilder.buildSporringMessage(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns sporringXml
        every { infotrygdMqService.sendInfotrygdSporring(sporringXml, sykmeldingId) } returns
            correlationId
        every { processingStateService.updateStep(any(), any(), any()) } just Runs

        // When
        infotrygdService.retrySporring(state)

        // Then
        verify(exactly = 1) { gcpBucketService.getFellesformat(sykmeldingId) }
        verify(exactly = 1) { infotrygdMqService.sendInfotrygdSporring(sporringXml, sykmeldingId) }
        verify(exactly = 1) {
            processingStateService.updateStep(
                sykmeldingId = sykmeldingId,
                step = ProcessingStep.SPORRING_SENT,
                correlationId = correlationId
            )
        }
    }

    @Test
    fun `should mark as failed when retry sporring fails`() {
        // Given
        val sykmeldingId = "test-sykmelding-123"
        val state =
            mockk<InfotrygdProcessingState> {
                every { this@mockk.sykmeldingId } returns sykmeldingId
                every { pasientFnr } returns "12345678901"
                every { behandlerFnr } returns "98765432109"
                every { tssId } returns "12345"
                every { retryCount } returns 1
            }

        every { gcpBucketService.getFellesformat(sykmeldingId) } throws
            RuntimeException("GCP error")
        every { processingStateService.markFailed(any(), any()) } just Runs

        // When & Then
        assertThrows<RuntimeException> { infotrygdService.retrySporring(state) }

        verify(exactly = 1) { processingStateService.markFailed(sykmeldingId, any()) }
    }
}

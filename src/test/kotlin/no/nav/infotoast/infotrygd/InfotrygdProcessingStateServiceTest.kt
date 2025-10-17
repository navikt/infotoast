//package no.nav.infotoast.infotrygd
//
//import java.time.LocalDateTime
//import no.nav.infotoast.config.ValkeyConfiguration
//import org.assertj.core.api.Assertions.assertThat
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.data.redis.core.RedisTemplate
//import org.springframework.test.context.DynamicPropertyRegistry
//import org.springframework.test.context.DynamicPropertySource
//import org.testcontainers.containers.GenericContainer
//import org.testcontainers.junit.jupiter.Container
//import org.testcontainers.junit.jupiter.Testcontainers
//import org.testcontainers.utility.DockerImageName
//
//@SpringBootTest(
//    classes =
//        [
//            ValkeyConfiguration::class,
//            RedisAutoConfiguration::class,
//            InfotrygdProcessingStateService::class
//        ],
//    properties = ["spring.main.web-application-type=none"]
//)
//@Testcontainers
//class InfotrygdProcessingStateServiceTest {
//
//    companion object {
//        @Container
//        val redis: GenericContainer<*> =
//            GenericContainer(DockerImageName.parse("valkey/valkey:8.0"))
//                .withExposedPorts(6379)
//                .withReuse(true)
//
//        @JvmStatic
//        @DynamicPropertySource
//        fun redisProperties(registry: DynamicPropertyRegistry) {
//            registry.add("spring.data.redis.host") { redis.host }
//            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
//        }
//    }
//
//    @Autowired private lateinit var stateService: InfotrygdProcessingStateService
//
//    @Autowired private lateinit var redisTemplate: RedisTemplate<String, Any>
//
//    @BeforeEach
//    fun cleanup() {
//        // Clean up all keys before each test
//        redisTemplate.connectionFactory?.connection?.serverCommands()?.flushAll()
//    }
//
//    @Test
//    fun `should save and retrieve processing state`() {
//        // Given
//        val state =
//            InfotrygdProcessingState(
//                sykmeldingId = "test-sykmelding-123",
//                journalpostId = "journal-456",
//                tssId = "tss-789",
//                helsepersonellKategori = "LE",
//                navKontorNr = "0219",
//                pasientFnr = "12345678901",
//                behandlerFnr = "98765432109",
//                currentStep = ProcessingStep.INITIATED
//            )
//
//        // When
//        stateService.saveState(state)
//
//        // Then
//        val retrieved = stateService.getState("test-sykmelding-123")
//        assertThat(retrieved).isNotNull
//        assertThat(retrieved?.sykmeldingId).isEqualTo("test-sykmelding-123")
//        assertThat(retrieved?.journalpostId).isEqualTo("journal-456")
//        assertThat(retrieved?.tssId).isEqualTo("tss-789")
//        assertThat(retrieved?.currentStep).isEqualTo(ProcessingStep.INITIATED)
//    }
//
//    @Test
//    fun `should return null for non-existent state`() {
//        // When
//        val retrieved = stateService.getState("non-existent-id")
//
//        // Then
//        assertThat(retrieved).isNull()
//    }
//
//    @Test
//    fun `should update processing step`() {
//        // Given
//        val state =
//            InfotrygdProcessingState(
//                sykmeldingId = "test-sykmelding-456",
//                journalpostId = "journal-789",
//                tssId = "tss-123",
//                helsepersonellKategori = "LE",
//                navKontorNr = "0219",
//                pasientFnr = "12345678901",
//                behandlerFnr = "98765432109",
//                currentStep = ProcessingStep.INITIATED
//            )
//        stateService.saveState(state)
//
//        // When
//        stateService.updateStep("test-sykmelding-456", ProcessingStep.SPORRING_SENT, "corr-123")
//
//        // Then
//        val updated = stateService.getState("test-sykmelding-456")
//        assertThat(updated?.currentStep).isEqualTo(ProcessingStep.SPORRING_SENT)
//        assertThat(updated?.sporringCorrelationId).isEqualTo("corr-123")
//        assertThat(updated?.updatedAt).isAfter(state.createdAt)
//    }
//
//    @Test
//    fun `should update oppdatering step with correlation id`() {
//        // Given
//        val state =
//            InfotrygdProcessingState(
//                sykmeldingId = "test-sykmelding-789",
//                journalpostId = "journal-123",
//                tssId = "tss-456",
//                helsepersonellKategori = "LE",
//                navKontorNr = "0219",
//                pasientFnr = "12345678901",
//                behandlerFnr = "98765432109",
//                currentStep = ProcessingStep.SPORRING_RECEIVED,
//                sporringCorrelationId = "corr-sporring-123"
//            )
//        stateService.saveState(state)
//
//        // When
//        stateService.updateStep(
//            "test-sykmelding-789",
//            ProcessingStep.OPPDATERING_SENT,
//            "corr-oppdatering-456"
//        )
//
//        // Then
//        val updated = stateService.getState("test-sykmelding-789")
//        assertThat(updated?.currentStep).isEqualTo(ProcessingStep.OPPDATERING_SENT)
//        assertThat(updated?.oppdateringCorrelationId).isEqualTo("corr-oppdatering-456")
//        assertThat(updated?.sporringCorrelationId).isEqualTo("corr-sporring-123")
//    }
//
//    @Test
//    fun `should mark processing as failed`() {
//        // Given
//        val state =
//            InfotrygdProcessingState(
//                sykmeldingId = "test-sykmelding-error",
//                journalpostId = "journal-error",
//                tssId = "tss-error",
//                helsepersonellKategori = "LE",
//                navKontorNr = "0219",
//                pasientFnr = "12345678901",
//                behandlerFnr = "98765432109",
//                currentStep = ProcessingStep.SPORRING_SENT
//            )
//        stateService.saveState(state)
//
//        // When
//        stateService.markFailed("test-sykmelding-error", "Connection timeout")
//
//        // Then
//        val failed = stateService.getState("test-sykmelding-error")
//        assertThat(failed?.currentStep).isEqualTo(ProcessingStep.FAILED)
//        assertThat(failed?.errorMessage).isEqualTo("Connection timeout")
//    }
//
//    @Test
//    fun `should store and retrieve correlation mapping`() {
//        // Given
//        val state =
//            InfotrygdProcessingState(
//                sykmeldingId = "test-sykmelding-correlation",
//                journalpostId = "journal-correlation",
//                tssId = "tss-correlation",
//                helsepersonellKategori = "LE",
//                navKontorNr = "0219",
//                pasientFnr = "12345678901",
//                behandlerFnr = "98765432109",
//                currentStep = ProcessingStep.INITIATED
//            )
//        stateService.saveState(state)
//
//        // When
//        stateService.updateStep(
//            "test-sykmelding-correlation",
//            ProcessingStep.SPORRING_SENT,
//            "unique-correlation-id-123"
//        )
//
//        // Then
//        val sykmeldingId = stateService.getSykmeldingIdByCorrelation("unique-correlation-id-123")
//        assertThat(sykmeldingId).isEqualTo("test-sykmelding-correlation")
//    }
//
//    @Test
//    fun `should return null for non-existent correlation id`() {
//        // When
//        val sykmeldingId = stateService.getSykmeldingIdByCorrelation("non-existent-correlation")
//
//        // Then
//        assertThat(sykmeldingId).isNull()
//    }
//
//    @Test
//    fun `should handle multiple correlation ids for same sykmelding`() {
//        // Given
//        val state =
//            InfotrygdProcessingState(
//                sykmeldingId = "test-sykmelding-multi-corr",
//                journalpostId = "journal-multi",
//                tssId = "tss-multi",
//                helsepersonellKategori = "LE",
//                navKontorNr = "0219",
//                pasientFnr = "12345678901",
//                behandlerFnr = "98765432109",
//                currentStep = ProcessingStep.INITIATED
//            )
//        stateService.saveState(state)
//
//        // When - first correlation for sporring
//        stateService.updateStep(
//            "test-sykmelding-multi-corr",
//            ProcessingStep.SPORRING_SENT,
//            "sporring-corr-123"
//        )
//
//        // And - second correlation for oppdatering
//        stateService.updateStep(
//            "test-sykmelding-multi-corr",
//            ProcessingStep.OPPDATERING_SENT,
//            "oppdatering-corr-456"
//        )
//
//        // Then - both correlations should map to same sykmelding
//        val sykmeldingId1 = stateService.getSykmeldingIdByCorrelation("sporring-corr-123")
//        val sykmeldingId2 = stateService.getSykmeldingIdByCorrelation("oppdatering-corr-456")
//        assertThat(sykmeldingId1).isEqualTo("test-sykmelding-multi-corr")
//        assertThat(sykmeldingId2).isEqualTo("test-sykmelding-multi-corr")
//
//        // And state should have both correlation ids
//        val finalState = stateService.getState("test-sykmelding-multi-corr")
//        assertThat(finalState?.sporringCorrelationId).isEqualTo("sporring-corr-123")
//        assertThat(finalState?.oppdateringCorrelationId).isEqualTo("oppdatering-corr-456")
//    }
//
//    @Test
//    fun `should preserve all fields when updating step`() {
//        // Given
//        val state =
//            InfotrygdProcessingState(
//                sykmeldingId = "test-preserve",
//                journalpostId = "journal-preserve",
//                tssId = "tss-preserve",
//                helsepersonellKategori = "LE",
//                navKontorNr = "0219",
//                pasientFnr = "12345678901",
//                behandlerFnr = "98765432109",
//                currentStep = ProcessingStep.INITIATED,
//                retryCount = 2
//            )
//        stateService.saveState(state)
//
//        // When
//        stateService.updateStep("test-preserve", ProcessingStep.SPORRING_SENT, "corr-preserve")
//
//        // Then
//        val updated = stateService.getState("test-preserve")
//        assertThat(updated?.journalpostId).isEqualTo("journal-preserve")
//        assertThat(updated?.tssId).isEqualTo("tss-preserve")
//        assertThat(updated?.helsepersonellKategori).isEqualTo("LE")
//        assertThat(updated?.navKontorNr).isEqualTo("0219")
//        assertThat(updated?.pasientFnr).isEqualTo("12345678901")
//        assertThat(updated?.behandlerFnr).isEqualTo("98765432109")
//        assertThat(updated?.retryCount).isEqualTo(2)
//    }
//
//    @Test
//    fun `should handle state overwrite correctly`() {
//        // Given
//        val state1 =
//            InfotrygdProcessingState(
//                sykmeldingId = "test-overwrite",
//                journalpostId = "journal-1",
//                tssId = "tss-1",
//                helsepersonellKategori = "LE",
//                navKontorNr = "0219",
//                pasientFnr = "12345678901",
//                behandlerFnr = "98765432109",
//                currentStep = ProcessingStep.INITIATED
//            )
//        stateService.saveState(state1)
//
//        // When - save a new state with same ID
//        val state2 =
//            state1.copy(
//                journalpostId = "journal-2",
//                currentStep = ProcessingStep.SPORRING_SENT,
//                updatedAt = LocalDateTime.now()
//            )
//        stateService.saveState(state2)
//
//        // Then - should have the latest state
//        val retrieved = stateService.getState("test-overwrite")
//        assertThat(retrieved?.journalpostId).isEqualTo("journal-2")
//        assertThat(retrieved?.currentStep).isEqualTo(ProcessingStep.SPORRING_SENT)
//    }
//}

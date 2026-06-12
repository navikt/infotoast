package no.nav.infotoast.config

import java.time.LocalDateTime
import no.nav.infotoast.infotrygd.InfotrygdProcessingState
import no.nav.infotoast.infotrygd.ProcessingStep
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest(
    classes =
        [
            ValkeyConfiguration::class,
            org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration::class,
            org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration::class
        ],
    properties = ["spring.main.web-application-type=none"]
)
@Testcontainers
class ValkeyConfigurationTest {

    companion object {
        @Container
        val redis: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("valkey/valkey:8.0"))
                .withExposedPorts(6379)
                .withReuse(true)

        @JvmStatic
        @DynamicPropertySource
        fun redisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
        }
    }

    @Autowired private lateinit var redisTemplate: RedisTemplate<String, Any>

    @BeforeEach
    fun cleanup() {
        redisTemplate.connectionFactory?.connection?.serverCommands()?.flushAll()
    }

    @Test
    fun `redisTemplate should be configured and connected`() {
        // When
        val connection = redisTemplate.connectionFactory?.connection

        // Then
        assertThat(connection).isNotNull
        assertThat(connection?.ping()).isEqualTo("PONG")
    }

    @Test
    fun `should serialize and deserialize complex objects with JSON`() {
        // Given
        val key = "test:complex:object"
        val state =
            InfotrygdProcessingState(
                sykmeldingId = "sykmelding-123",
                journalpostId = "journal-456",
                tssId = "tss-789",
                helsepersonellKategori = "LE",
                navKontorNr = "0219",
                pasientFnr = "12345678901",
                behandlerFnr = "98765432109",
                currentStep = ProcessingStep.SPORRING_SENT,
                sporringCorrelationId = "corr-123",
                oppdateringCorrelationId = "corr-456",
                errorMessage = "Test error",
                retryCount = 3
            )

        // When
        redisTemplate.opsForValue().set(key, state)
        val retrieved = redisTemplate.opsForValue().get(key)

        // Then
        assertThat(retrieved).isInstanceOf(InfotrygdProcessingState::class.java)
        val retrievedState = retrieved as InfotrygdProcessingState
        assertThat(retrievedState.sykmeldingId).isEqualTo("sykmelding-123")
        assertThat(retrievedState.journalpostId).isEqualTo("journal-456")
        assertThat(retrievedState.currentStep).isEqualTo(ProcessingStep.SPORRING_SENT)
        assertThat(retrievedState.sporringCorrelationId).isEqualTo("corr-123")
        assertThat(retrievedState.errorMessage).isEqualTo("Test error")
        assertThat(retrievedState.retryCount).isEqualTo(3)
    }

    @Test
    fun `should serialize LocalDateTime correctly`() {
        // Given
        val key = "test:datetime"
        val now = LocalDateTime.now()
        val state =
            InfotrygdProcessingState(
                sykmeldingId = "sykmelding-datetime",
                journalpostId = "journal-datetime",
                tssId = "tss-datetime",
                helsepersonellKategori = "LE",
                navKontorNr = "0219",
                pasientFnr = "12345678901",
                behandlerFnr = "98765432109",
                currentStep = ProcessingStep.INITIATED,
                createdAt = now,
                updatedAt = now
            )

        // When
        redisTemplate.opsForValue().set(key, state)
        val retrieved = redisTemplate.opsForValue().get(key) as InfotrygdProcessingState

        // Then
        assertThat(retrieved.createdAt).isEqualTo(now)
        assertThat(retrieved.updatedAt).isEqualTo(now)
    }

    @Test
    fun `should handle null values in optional fields`() {
        // Given
        val key = "test:nulls"
        val state =
            InfotrygdProcessingState(
                sykmeldingId = "sykmelding-nulls",
                journalpostId = "journal-nulls",
                tssId = "tss-nulls",
                helsepersonellKategori = "LE",
                navKontorNr = "0219",
                pasientFnr = "12345678901",
                behandlerFnr = "98765432109",
                currentStep = ProcessingStep.INITIATED,
                sporringCorrelationId = null,
                oppdateringCorrelationId = null,
                errorMessage = null
            )

        // When
        redisTemplate.opsForValue().set(key, state)
        val retrieved = redisTemplate.opsForValue().get(key) as InfotrygdProcessingState

        // Then
        assertThat(retrieved.sporringCorrelationId).isNull()
        assertThat(retrieved.oppdateringCorrelationId).isNull()
        assertThat(retrieved.errorMessage).isNull()
    }

    @Test
    fun `should serialize enum values correctly`() {
        // Given
        val key = "test:enum"
        ProcessingStep.values().forEach { step ->
            val state =
                InfotrygdProcessingState(
                    sykmeldingId = "sykmelding-enum-$step",
                    journalpostId = "journal-enum",
                    tssId = "tss-enum",
                    helsepersonellKategori = "LE",
                    navKontorNr = "0219",
                    pasientFnr = "12345678901",
                    behandlerFnr = "98765432109",
                    currentStep = step
                )

            // When
            redisTemplate.opsForValue().set("$key:$step", state)
            val retrieved =
                redisTemplate.opsForValue().get("$key:$step") as InfotrygdProcessingState

            // Then
            assertThat(retrieved.currentStep).isEqualTo(step)
        }
    }

    @Test
    fun `should handle String keys correctly`() {
        // Given
        val keys =
            listOf(
                "simple-key",
                "key:with:colons",
                "key/with/slashes",
                "key-with-dashes",
                "key_with_underscores"
            )

        // When/Then
        keys.forEach { key ->
            redisTemplate.opsForValue().set(key, "test-value-$key")
            val retrieved = redisTemplate.opsForValue().get(key)
            assertThat(retrieved).isEqualTo("test-value-$key")
        }
    }

    @Test
    fun `should support basic Redis operations`() {
        // Given
        val key = "test:operations"

        // When/Then - Set and Get
        redisTemplate.opsForValue().set(key, "value1")
        assertThat(redisTemplate.opsForValue().get(key)).isEqualTo("value1")

        // Delete
        redisTemplate.delete(key)
        assertThat(redisTemplate.hasKey(key)).isFalse()

        // Has key
        redisTemplate.opsForValue().set(key, "value2")
        assertThat(redisTemplate.hasKey(key)).isTrue()
    }

    @Test
    fun `should support TTL operations`() {
        // Given
        val key = "test:ttl"
        val state =
            InfotrygdProcessingState(
                sykmeldingId = "sykmelding-ttl",
                journalpostId = "journal-ttl",
                tssId = "tss-ttl",
                helsepersonellKategori = "LE",
                navKontorNr = "0219",
                pasientFnr = "12345678901",
                behandlerFnr = "98765432109",
                currentStep = ProcessingStep.INITIATED
            )

        // When
        redisTemplate.opsForValue().set(key, state, java.time.Duration.ofSeconds(10))

        // Then
        val ttl = redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.SECONDS)
        assertThat(ttl).isGreaterThan(0).isLessThanOrEqualTo(10)
    }

    @Test
    fun `should handle concurrent access correctly`() {
        // Given
        val key = "test:concurrent"
        val threads = 10
        val iterations = 10

        // When - Multiple threads writing to different keys
        val threadList =
            (1..threads).map { threadId ->
                Thread {
                    repeat(iterations) { iteration ->
                        val threadKey = "$key:thread-$threadId:iter-$iteration"
                        redisTemplate.opsForValue().set(threadKey, "value-$threadId-$iteration")
                    }
                }
            }

        threadList.forEach { it.start() }
        threadList.forEach { it.join() }

        // Then - All values should be present
        repeat(threads) { threadId ->
            repeat(iterations) { iteration ->
                val threadKey = "$key:thread-${threadId + 1}:iter-$iteration"
                val value = redisTemplate.opsForValue().get(threadKey)
                assertThat(value).isEqualTo("value-${threadId + 1}-$iteration")
            }
        }
    }
}

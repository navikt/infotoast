package no.nav.tsm

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test
import no.nav.tsm.ktor.kafka.test.KafkaContainer
import no.nav.tsm.ktor.kafka.test.send
import no.nav.tsm.sykmelding.input.core.model.Aktivitet
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarArsakType
import no.nav.tsm.sykmelding.input.core.model.ArbeidsgiverInfo
import no.nav.tsm.sykmelding.input.core.model.AvsenderSystem
import no.nav.tsm.sykmelding.input.core.model.Behandler
import no.nav.tsm.sykmelding.input.core.model.DiagnoseInfo
import no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem
import no.nav.tsm.sykmelding.input.core.model.DigitalPrognose
import no.nav.tsm.sykmelding.input.core.model.MedisinskVurdering
import no.nav.tsm.sykmelding.input.core.model.Pasient
import no.nav.tsm.sykmelding.input.core.model.Reason
import no.nav.tsm.sykmelding.input.core.model.Rule
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.Sporsmalstype
import no.nav.tsm.sykmelding.input.core.model.Sykmelder
import no.nav.tsm.sykmelding.input.core.model.Sykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingMeta
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.UtdypendeSporsmal
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.ValidationType
import no.nav.tsm.sykmelding.input.core.model.metadata.HelsepersonellKategori
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata
import no.nav.tsm.sykmelding.input.core.model.metadata.Navn
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonId
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType
import no.nav.tsm.utils.configureFullIntegrationTests
import org.apache.kafka.clients.producer.KafkaProducer
import tools.jackson.module.kotlin.jacksonObjectMapper

const val SYKMELDINGER_TOPIC = "tsm.sykmeldinger"

class IntegrationTest {
    val kafka = KafkaContainer(createTopics = listOf(SYKMELDINGER_TOPIC))

    val producer: KafkaProducer<String, ByteArray> = kafka.createAnythingProducer()

    val mapper = jacksonObjectMapper()

    private fun ApplicationTestBuilder.configureEverythingTest() {
        kafka.configureKafka(this)

        configureFullIntegrationTests()
    }

    @Test
    fun `Should consume from kafka topic, and not throw any exceptions`() = testApplication {
        configureEverythingTest()

        val key = UUID.randomUUID().toString()

        val record =
            SykmeldingRecord.Digital(
                metadata = MessageMetadata.Digital(orgnummer = "123456789"),
                sykmelding =
                    Sykmelding.Digital(
                        id = key,
                        metadata =
                            SykmeldingMeta.Digital(
                                genDate = OffsetDateTime.now(),
                                mottattDato = OffsetDateTime.now(),
                                avsenderSystem =
                                    AvsenderSystem(navn = "Test System", versjon = "1.0.0"),
                            ),
                        pasient =
                            Pasient(
                                fnr = "12345678901",
                                kontaktinfo = emptyList(),
                                navn = null,
                                navKontor = null,
                                navnFastlege = null,
                            ),
                        medisinskVurdering =
                            MedisinskVurdering.Digital(
                                hovedDiagnose =
                                    DiagnoseInfo(
                                        kode = "A01",
                                        system = DiagnoseSystem.ICPC2,
                                        tekst = "Test diagnose",
                                    ),
                                biDiagnoser =
                                    listOf(
                                        DiagnoseInfo(
                                            kode = "B02",
                                            system = DiagnoseSystem.ICD10,
                                            tekst = "Bi-diagnose test",
                                        )
                                    ),
                                svangerskap = false,
                                skjermetForPasient = false,
                                yrkesskade = null,
                                annenFravarsgrunn = AnnenFravarArsakType.ARBEIDSRETTET_TILTAK,
                            ),
                        aktivitet =
                            listOf(
                                Aktivitet.IkkeMulig(
                                    fom = OffsetDateTime.now().minusDays(15).toLocalDate(),
                                    tom = OffsetDateTime.now().minusDays(10).toLocalDate(),
                                    medisinskArsak = null,
                                    arbeidsrelatertArsak = null,
                                ),
                                Aktivitet.Gradert(
                                    fom = OffsetDateTime.now().minusDays(11).toLocalDate(),
                                    tom = OffsetDateTime.now().minusDays(5).toLocalDate(),
                                    grad = 69,
                                    reisetilskudd = false,
                                ),
                            ),
                        behandler =
                            Behandler(
                                navn =
                                    Navn(
                                        fornavn = "Ola",
                                        mellomnavn = null,
                                        etternavn = "Nordmann",
                                    ),
                                adresse = null,
                                ids =
                                    listOf(
                                        PersonId(type = PersonIdType.HPR, id = "123456"),
                                        PersonId(type = PersonIdType.FNR, id = "23123213131"),
                                    ),
                                kontaktinfo = emptyList(),
                            ),
                        sykmelder =
                            Sykmelder(
                                ids = listOf(PersonId(type = PersonIdType.HPR, id = "123456")),
                                helsepersonellKategori = HelsepersonellKategori.LEGE,
                            ),
                        arbeidsgiver =
                            ArbeidsgiverInfo.En(
                                navn = "Test AS",
                                meldingTilArbeidsgiver = "Må sitte mer",
                                tiltakArbeidsplassen = null,
                                yrkesbetegnelse = null,
                                stillingsprosent = null,
                            ),
                        tilbakedatering = null,
                        bistandNav = null,
                        utdypendeSporsmal =
                            listOf(
                                UtdypendeSporsmal(
                                    svar = "Dette er et svar",
                                    type = Sporsmalstype.MEDISINSKE_HENSYN,
                                    skjermetForArbeidsgiver = false,
                                    sporsmal = "Dette er et spørsmål",
                                )
                            ),
                        prognose = DigitalPrognose(friskmeldingTilArbeidsformidling = false),
                    ),
                validation =
                    ValidationResult(
                        status = RuleType.OK,
                        timestamp = OffsetDateTime.now(),
                        rules =
                            listOf(
                                Rule.OK(
                                    name = "Test rule",
                                    timestamp = OffsetDateTime.now(),
                                    validationType = ValidationType.MANUAL,
                                ),
                                Rule.Pending(
                                    name = "Test rule",
                                    timestamp = OffsetDateTime.now().minusDays(1),
                                    validationType = ValidationType.AUTOMATIC,
                                    reason =
                                        Reason(
                                            sykmeldt = "Melding til sykmeldt",
                                            sykmelder = "Melding til sykmelder",
                                        ),
                                ),
                            ),
                    ),
            )

        startApplication()

        shouldNotThrowAny {
            producer.send(
                topic = SYKMELDINGER_TOPIC,
                key = key,
                value = mapper.writeValueAsBytes(record),
            )
        }
    }
}

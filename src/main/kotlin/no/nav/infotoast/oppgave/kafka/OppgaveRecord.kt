package no.nav.infotoast.oppgave.kafka

data class OppgaveRecord(
    val produserOppgave: ProduserOppgaveKafkaMessage,
    val journalOpprettet: JournalKafkaMessage,
)

data class ProduserOppgaveKafkaMessage(
    val messageId: String,
    val aktoerId: String,
    val tildeltEnhetsnr: String,
    val opprettetAvEnhetsnr: String,
    val behandlesAvApplikasjon: String,
    val orgnr: String,
    val beskrivelse: String,
    val temagruppe: String,
    val tema: String,
    val behandlingstema: String,
    val oppgavetype: String,
    val behandlingstype: String,
    val mappeId: Int,
    val aktivDato: String,
    val fristFerdigstillelse: String,
    val prioritet: PrioritetType,
    val metadata: Map<String?, String?>,
)

data class JournalKafkaMessage(
    val messageId: String,
    val journalpostId: String,
    val journalpostKilde: String,
)

enum class PrioritetType {
    HOY,
    NORM,
    LAV
}

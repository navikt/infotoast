package no.nav.tsm.modules.sykmelding

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.core.Environment
import no.nav.tsm.ktor.kafka.consumer.KafkaConsumer
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord

fun Application.configureSykmelding() {
    configureConsumer()
}

private fun Application.configureConsumer() {

    val env: Environment by dependencies

    install(KafkaConsumer) {
        clientId = env.runtime.name
        groupId = "infotoast-consumer"
        consume<SykmeldingRecord>(
            name = "tsm.sykmeldinger-input",
            onRecord = { record ->
                log.info("Received a Sykmelding for ID: ${record.sykmelding.id}")
            },
            onTombstone = { meta ->
                log.info("Received a Sykmelding tombstone for ID ${meta.key}, skips")
            },
        )
        jacksonModule(SykmeldingModule())
    }
}

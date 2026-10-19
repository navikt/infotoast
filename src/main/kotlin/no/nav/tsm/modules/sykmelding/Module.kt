package no.nav.tsm.modules.sykmelding

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.core.Environment
import no.nav.tsm.ktor.kafka.sykmeldinger.SykmeldingerConsumer

fun Application.configureSykmelding() {
    configureConsumer()
}

private fun Application.configureConsumer() {

    val env: Environment by dependencies

    install(SykmeldingerConsumer) {
        clientId = env.runtime.name
        groupId = "infotoast-consumer"
        onRecord = { record, _ -> }
        onTombstone = { meta ->
            log.info("Received a Sykmelding tombstone for ID ${meta.key}, skips")
        }
    }
}

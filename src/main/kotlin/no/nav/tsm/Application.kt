package no.nav.tsm

import io.ktor.server.application.*
import no.nav.tsm.modules.sykmelding.configureSykmelding
import no.nav.tsm.plugins.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureDependencies()
    configureMonitoring()

    configureSykmelding()
}

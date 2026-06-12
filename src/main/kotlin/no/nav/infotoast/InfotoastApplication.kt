package no.nav.infotoast

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication class InfotoastApplication

fun main(args: Array<String>) {
    runApplication<InfotoastApplication>(*args)
}

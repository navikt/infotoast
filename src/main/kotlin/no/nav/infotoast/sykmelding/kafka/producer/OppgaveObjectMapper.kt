package no.nav.infotoast.sykmelding.kafka.producer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

val oppgaveObjectMapper =
    jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

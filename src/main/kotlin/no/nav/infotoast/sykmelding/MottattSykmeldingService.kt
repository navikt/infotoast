package no.nav.infotoast.sykmelding

import java.time.LocalDate
import org.springframework.stereotype.Service

@Service
class MottattSykmeldingService {
    // TODO implement
}

fun List<Aktivitet>.sortedFOMDate(): List<LocalDate> = map { it.fom }.sorted()

fun List<Aktivitet>.sortedTOMDate(): List<LocalDate> = map { it.tom }.sorted()

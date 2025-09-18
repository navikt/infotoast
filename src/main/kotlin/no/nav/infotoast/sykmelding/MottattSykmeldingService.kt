package no.nav.infotoast.sykmelding

import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class MottattSykmeldingService {
    //TODO implement
}

fun List<Aktivitet>.sortedFOMDate(): List<LocalDate> = map { it.fom }.sorted()

fun List<Aktivitet>.sortedTOMDate(): List<LocalDate> = map { it.tom }.sorted()
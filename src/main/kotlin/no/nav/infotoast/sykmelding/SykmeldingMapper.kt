package no.nav.tsm.syk_inn_api.sykmelding

import java.time.LocalDate
import java.time.Month
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord

fun SykmeldingRecord.isBeforeYear(year: Int): Boolean {
    val tom = sykmelding.aktivitet.maxBy { it.tom }.tom
    return tom.isBefore(
        LocalDate.of(
            year,
            Month.JANUARY,
            1,
        ),
    )
}

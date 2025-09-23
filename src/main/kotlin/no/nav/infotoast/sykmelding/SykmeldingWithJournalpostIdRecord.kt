package no.nav.infotoast.sykmelding

import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord

data class SykmeldingWithJournalpostIdRecord(
    val sykmeldingRecord: SykmeldingRecord,
    val journalpostId: String,
)
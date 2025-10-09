package no.nav.infotoast.norg

import no.nav.infotoast.person.Person
import no.nav.infotoast.person.getDiskresjonskode
import no.nav.infotoast.utils.logger
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.UtenlandskSykmelding
import org.springframework.stereotype.Service

@Service
class NavKontorService(private val norg2Client: INorg2Client) {
    private val logger = logger()

    companion object {
        const val UTENLANDSK_NAV_KONTOR = "2101" // NAV office for all utenlandsk sykmeldinger
    }

    /**
     * Finds the local NAV office number (enhetNr) for a person based on their geografisk
     * tilknytning (GT) and diskresjonskode from PDL.
     *
     * For utenlandsk sykmeldinger, always returns 2101 (the dedicated NAV office).
     *
     * @param person The PDL person data containing GT and adressebeskyttelse
     * @param sykmeldingRecord The sykmelding record to check if it's utenlandsk
     * @param sykmeldingId The sykmelding ID for logging/tracing
     * @return The NAV office number (enhetNr)
     * @throws IllegalStateException if NAV office cannot be determined
     */
    fun finnLokaltNavkontor(
        person: Person,
        sykmeldingRecord: SykmeldingRecord,
        sykmeldingId: String
    ): String {
        logger.info("Looking up local NAV office for sykmelding $sykmeldingId")

        // Check if this is an utenlandsk sykmelding
        if (sykmeldingRecord.sykmelding is UtenlandskSykmelding) {
            logger.info(
                "Sykmelding $sykmeldingId is utenlandsk, using NAV kontor $UTENLANDSK_NAV_KONTOR"
            )
            return UTENLANDSK_NAV_KONTOR
        }

        val geografiskTilknytning = person.gt
        val diskresjonskode = person.getDiskresjonskode()

        logger.info(
            "Person has GT=$geografiskTilknytning, diskresjonskode=$diskresjonskode for sykmelding $sykmeldingId"
        )

        val result =
            norg2Client.getLocalNAVOffice(
                gt = geografiskTilknytning,
                diskresjonskode = diskresjonskode
            )

        if (result.isFailure) {
            val error = result.exceptionOrNull()
            logger.error("Failed to lookup NAV office for sykmelding $sykmeldingId", error)
            throw IllegalStateException("Could not determine local NAV office", error)
        }

        val navOffice = result.getOrNull()
        requireNotNull(navOffice) { "NAV office is null for sykmelding $sykmeldingId" }

        logger.info("Found NAV office ${navOffice.enhetNr} for sykmelding $sykmeldingId")
        return navOffice.enhetNr
    }
}

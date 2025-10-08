package no.nav.infotoast.gcp

import no.nav.helse.eiFellesformat.XMLEIFellesformat

interface IGcpBucketService {
    /**
     * Downloads the fellesformat XML for a sykmelding from GCP bucket
     *
     * @param sykmeldingId the ID of the sykmelding
     * @return XMLEIFellesformat if found, null otherwise
     */
    fun getFellesformat(sykmeldingId: String): XMLEIFellesformat?

    /** Checks if a fellesformat exists in the bucket for the given sykmeldingId */
    fun fellesformatExists(sykmeldingId: String): Boolean
}

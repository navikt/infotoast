package no.nav.infotoast.gcp

import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.infotoast.utils.logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("local", "test")
@Service
class MockGcpBucketService : IGcpBucketService {
    private val logger = logger()

    override fun getFellesformat(sykmeldingId: String): XMLEIFellesformat {
        logger.info(
            "MockGcpBucketService: Returning mock fellesformat for sykmeldingId: $sykmeldingId"
        )

        // Return a basic mock XMLEIFellesformat object for local testing
        // In a real scenario, you could load this from a test resource file
        return XMLEIFellesformat()
    }

    override fun fellesformatExists(sykmeldingId: String): Boolean {
        logger.info(
            "MockGcpBucketService: Checking if fellesformat exists for sykmeldingId: $sykmeldingId"
        )
        // Always return true for local testing
        return true
    }
}

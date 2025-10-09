package no.nav.infotoast.norg

import no.nav.infotoast.utils.logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local", "test")
@Component
class MockNorg2Client : INorg2Client {
    private val logger = logger()

    override fun getLocalNAVOffice(gt: String?, diskresjonskode: String?): Result<Norg2Response> {
        logger.info(
            "MockNorg2Client: Returning mock NAV office for GT=$gt, diskresjonskode=$diskresjonskode"
        )

        // Return a mock NAV office number for local testing
        return Result.success(
            Norg2Response(
                enhetNr = "0314", // Mock NAV office number (e.g., NAV Oslo)
                navn = "NAV Oslo"
            )
        )
    }
}

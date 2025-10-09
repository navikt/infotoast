package no.nav.infotoast.sykmelder.hpr

import no.nav.infotoast.utils.logger
import org.springframework.stereotype.Service

@Service
class HprService(private val helsenettProxyClient: IHelsenettProxyClient) {
    private val logger = logger()

    /**
     * Gets the healthcare personnel category (helsepersonellkategori) for a behandler. Returns the
     * category code (e.g., "LE" for lege/doctor, "KI" for kiropraktor)
     *
     * @param behandlerFnr The national ID of the healthcare personnel
     * @param callId The sykmelding ID for logging/tracing
     * @return The personnel category code, or null if not found
     * @throws IllegalStateException if HPR lookup fails or category is not available
     */
    fun getHelsepersonellKategori(behandlerFnr: String, callId: String): String {
        logger.info("Looking up helsepersonellkategori for behandler, sykmeldingId=$callId")

        val hprResult = helsenettProxyClient.getSykmelderByFnr(behandlerFnr, callId)

        if (hprResult.isFailure) {
            val error = hprResult.exceptionOrNull()
            logger.error("Failed to get HPR data for behandler, sykmeldingId=$callId", error)
            throw IllegalStateException("Could not fetch HPR data for behandler", error)
        }

        val hprSykmelder = hprResult.getOrNull()
        requireNotNull(hprSykmelder) { "HPR sykmelder is null for sykmeldingId=$callId" }

        // Get the active helsepersonellkategori from godkjenninger
        val kategori =
            hprSykmelder.godkjenninger
                .firstOrNull { it.helsepersonellkategori?.aktiv == true }
                ?.helsepersonellkategori
                ?.verdi

        if (kategori == null) {
            logger.error(
                "No active helsepersonellkategori found for behandler, sykmeldingId=$callId"
            )
            // TODO: Determine if we should crash or use a default value
            // For now, throw an exception as per user request to check what to do when not
            // available
            throw IllegalStateException("No active helsepersonellkategori found for behandler")
        }

        logger.info("Found helsepersonellkategori=$kategori for behandler, sykmeldingId=$callId")
        return kategori
    }
}

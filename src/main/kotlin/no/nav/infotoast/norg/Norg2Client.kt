package no.nav.infotoast.norg

import no.nav.infotoast.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

interface INorg2Client {
    fun getLocalNAVOffice(gt: String?, diskresjonskode: String?): Result<Norg2Response>
}

@Profile("!local & !test")
@Component
class Norg2Client(
    restClientBuilder: RestClient.Builder,
    @param:Value("\${services.norg2.url}") private val baseUrl: String,
) : INorg2Client {
    private val logger = logger()

    private val restClient: RestClient = restClientBuilder.baseUrl(baseUrl).build()

    override fun getLocalNAVOffice(gt: String?, diskresjonskode: String?): Result<Norg2Response> {
        if (gt.isNullOrBlank()) {
            logger.error("Geografisk tilknytning (GT) is null or blank, cannot lookup NAV office")
            return Result.failure(IllegalArgumentException("GT is required to lookup NAV office"))
        }

        logger.info("Looking up local NAV office for GT=$gt, diskresjonskode=$diskresjonskode")

        return try {
            // NORG2 API endpoint: /norg2/api/v1/arbeidsfordeling/enheter/bestmatch
            val response =
                restClient
                    .post()
                    .uri("/norg2/api/v1/arbeidsfordeling/enheter/bestmatch")
                    .headers { it.set("Content-Type", "application/json") }
                    .body(
                        mapOf(
                            "geografiskOmraade" to gt,
                            "diskresjonskode" to diskresjonskode,
                        )
                    )
                    .retrieve()
                    .body(Norg2Response::class.java)

            if (response != null) {
                logger.info("Successfully found NAV office: ${response.enhetNr} for GT=$gt")
                Result.success(response)
            } else {
                val msg = "NORG2 returned null response for GT=$gt"
                logger.warn(msg)
                Result.failure(IllegalStateException("NORG2 returned no NAV office"))
            }
        } catch (e: RestClientResponseException) {
            val status = e.statusCode
            val body = e.responseBodyAsString
            logger.error("NORG2 HTTP ${status.value()}: $body, GT=$gt", e)
            Result.failure(IllegalStateException("NORG2 error (${status.value()}): $body", e))
        } catch (e: Exception) {
            logger.error("Error while calling NORG2 API", e)
            Result.failure(e)
        }
    }
}

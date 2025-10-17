package no.nav.infotoast.sykmelder.tss

import no.nav.infotoast.security.TexasClient
import no.nav.infotoast.utils.logger
import no.nav.infotoast.utils.teamLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

interface ISmTssClient {
    fun getTssId(fnr: String, orgName: String, sykmeldingId: String): Result<TssIdent>
}

// TODO test this client
@Profile("!local & !test")
@Component
class SmTssClient(
    restClientBuilder: RestClient.Builder,
    private val texasClient: TexasClient,
    @param:Value($$"${services.teamsykmelding.smtss.url}") private val baseUrl: String,
) : ISmTssClient {

    private val restClient: RestClient = restClientBuilder.baseUrl(baseUrl).build()

    private val logger = logger()
    private val teamLogger = teamLogger()

    override fun getTssId(fnr: String, orgName: String, sykmeldingId: String): Result<TssIdent> {
        val (accessToken) = getToken()

        return try {
            val response =
                restClient
                    .get()
                    .uri { it.build("api/v1/samhandler/infotrygd") }
                    .headers {
                        it.set("requestId", sykmeldingId)
                        it.set("samhandlerFnr", fnr)
                        it.set("samhandlerOrgName", orgName)
                        it.set("Authorization", "Bearer $accessToken")
                        it.set("Content-Type", "application/json")
                    }
                    .retrieve()
                    .body(TssIdent::class.java)
            if (response != null) {
                Result.success(response)
            } else {
                Result.failure(IllegalStateException("smtss did not return a tssId"))
            }
        } catch (e: RestClientResponseException) {
            val status = e.statusCode
            val body = e.responseBodyAsString

            when {
                status.value() == 404 -> {
                    teamLogger.warn(
                        "No tssId found for Sykmelder with fnr $fnr found in smtss. Body: $body",
                        e
                    )
                    logger.warn("No tssId available for sykmelder in smtss", e)
                    Result.failure(IllegalStateException("Could not find tssId in smtss"))
                }
                status.is4xxClientError -> {
                    teamLogger.error("SmTss client error ${status.value()}: $body, fnr: $fnr", e)
                    Result.failure(
                        IllegalStateException("SmTss client error (${status.value()}): $body")
                    )
                }
                status.is5xxServerError -> {
                    teamLogger.error("SmTss server error ${status.value()}: $body, fnr: $fnr", e)
                    Result.failure(
                        IllegalStateException("SmTss server error (${status.value()}): $body")
                    )
                }
                else -> {
                    teamLogger.error(
                        "SmTss unexpected HTTP status ${status.value()}: $body, fnr: $fnr",
                        e
                    )
                    Result.failure(
                        IllegalStateException("SmTss unexpected status (${status.value()}): $body")
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error while calling SmTss API", e)
            Result.failure(e)
        }
    }

    private fun getToken(): TexasClient.TokenResponse =
        texasClient.requestToken("teamsykmelding", "smtss")
}

data class TssIdent(
    val tssid: String,
)

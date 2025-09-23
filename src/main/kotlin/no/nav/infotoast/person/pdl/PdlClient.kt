package no.nav.infotoast.person.pdl

import no.nav.infotoast.security.TexasClient
import no.nav.infotoast.utils.logger
import no.nav.infotoast.utils.teamLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

interface IPdlClient {
    fun getPerson(fnr: String): Result<PdlPerson>
}

@Profile("!local & !test")
@Component
class PdlClient(
    restClientBuilder: RestClient.Builder,
    private val texasClient: TexasClient,
    @param:Value($$"${services.pdl.graphql.url}") private val pdlGraphqlEndpointPath: String,
) : IPdlClient {
    private val restClient = restClientBuilder.baseUrl(pdlGraphqlEndpointPath).build()
    private val logger = logger()
    private val teamLogger = teamLogger()

    private val temaHeader = "TEMA"
    private val tema = "SYM"

    override fun getPerson(fnr: String): Result<PdlPerson> {
        val (accessToken) = getToken()
        val graphqlQuery = PdlClient::class.java.getResource("/graphql/getPerson.graphql")?.readText()
            ?: throw IllegalStateException("Could not load getPerson.graphql")

        return try {
            val response =
                restClient
                    .post()
                    .body(graphqlQuery)
                    .headers {
                        it.set("Authorization", "Bearer $accessToken")
                        it.set("Behandlingsnummer", "B229")
                        it.set(temaHeader, tema)
                        it.set("Content-Type", "application/json")
                    }
                    .retrieve()
                    .body(PdlPerson::class.java)
            if (response != null) {
                Result.success(response)
            } else {
                Result.failure(IllegalStateException("Pdl cache did not return a person"))
            }
        } catch (e: RestClientResponseException) {
            val status = e.statusCode
            val body = e.responseBodyAsString

            when {
                status.value() == 404 -> {
                    teamLogger.warn("Person with fnr $fnr not found in PDL cache. Body: $body", e)
                    logger.warn("PDL person not found in PDL cache", e)
                    Result.failure(IllegalStateException("Could not find person in pdl cache"))
                }
                status.is4xxClientError -> {
                    teamLogger.error("PDL client error ${status.value()}: $body, fnr: $fnr", e)
                    Result.failure(
                        IllegalStateException("PDL client error (${status.value()}): $body")
                    )
                }
                status.is5xxServerError -> {
                    teamLogger.error("PDL server error ${status.value()}: $body, fnr: $fnr", e)
                    Result.failure(
                        IllegalStateException("PDL server error (${status.value()}): $body")
                    )
                }
                else -> {
                    teamLogger.error(
                        "PDL unexpected HTTP status ${status.value()}: $body, fnr: $fnr",
                        e
                    )
                    Result.failure(
                        IllegalStateException("PDL unexpected status (${status.value()}): $body")
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error while calling Pdl API", e)
            Result.failure(e)
        }
    }

    private fun getToken(): TexasClient.TokenResponse =
        texasClient.requestToken("tsm", "pdl?")
    //TODO this needs to be updated to the correct namesspace and otherApiAppName
}

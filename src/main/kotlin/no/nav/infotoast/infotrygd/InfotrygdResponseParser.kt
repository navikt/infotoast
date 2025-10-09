package no.nav.infotoast.infotrygd

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import no.nav.infotoast.utils.logger
import org.springframework.stereotype.Component

@Component
class InfotrygdResponseParser {
    private val logger = logger()
    private val xmlMapper = XmlMapper()

    /**
     * Parse sporring (query) response from Infotrygd Uses ObjectMapper to deserialize XML response
     * (same as syfosminfotrygd)
     *
     * The response is an InfotrygdForesp XML object that gets unmarshalled
     */
    fun parseSporringResponse(xmlResponse: String): InfotrygdSporringResponse {
        return try {
            // Deserialize XML to InfotrygdForespResponse object
            val forespResponse =
                xmlMapper.readValue(xmlResponse, InfotrygdForespResponse::class.java)

            // Extract identDato from the last sykmelding periode
            val identDato =
                forespResponse.sMhistorikk?.sykmelding?.lastOrNull()?.periode?.arbufoerFOM

            logger.info(
                "Parsed sporring response: identDato=$identDato, tkNummer=${forespResponse.tkNummer}"
            )

            InfotrygdSporringResponse(identDato = identDato, tkNummer = forespResponse.tkNummer)
        } catch (e: Exception) {
            logger.error("Failed to parse sporring XML response", e)
            logger.error("Raw XML response: $xmlResponse")

            // Return empty response on parse failure
            InfotrygdSporringResponse(identDato = null, tkNummer = null)
        }
    }
}

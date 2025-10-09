package no.nav.infotoast.infotrygd

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

/**
 * Infotrygd sporring (query) response structure Based on syfosminfotrygd response format
 *
 * The response is XML that gets deserialized into this structure
 */
@JacksonXmlRootElement(localName = "InfotrygdForesp")
@JsonIgnoreProperties(ignoreUnknown = true)
data class InfotrygdForespResponse(
    @JacksonXmlProperty(localName = "tkNummer") val tkNummer: String? = null,
    @JacksonXmlProperty(localName = "sMhistorikk") val sMhistorikk: SMHistorikk? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SMHistorikk(
    @JacksonXmlProperty(localName = "sykmelding") val sykmelding: List<SMInfo>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SMInfo(@JacksonXmlProperty(localName = "periode") val periode: PeriodeResponse? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PeriodeResponse(
    @JacksonXmlProperty(localName = "arbufoerFOM")
    val arbufoerFOM: String? = null // This is the identDato
)

/** Simplified response for our use case Extracted from InfotrygdForespResponse */
data class InfotrygdSporringResponse(val identDato: String?, val tkNummer: String?)

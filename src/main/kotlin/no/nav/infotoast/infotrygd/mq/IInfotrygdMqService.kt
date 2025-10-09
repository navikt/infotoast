package no.nav.infotoast.infotrygd.mq

/** Interface for Infotrygd MQ operations Allows for mock implementation in local/test profiles */
interface IInfotrygdMqService {
    /**
     * Sends an Infotrygd query (sporring/forespørsel) to check existing data
     *
     * @param xmlMessage The XML message to send
     * @param sykmeldingId The sykmelding ID for tracking
     * @return The correlation ID for tracking the response
     */
    fun sendInfotrygdSporring(xmlMessage: String, sykmeldingId: String): String

    /**
     * Sends an Infotrygd update (oppdatering) message (fire-and-forget)
     *
     * @param xmlMessage The XML message to send
     * @param sykmeldingId The sykmelding ID for tracking
     * @return The correlation ID
     */
    fun sendInfotrygdOppdatering(xmlMessage: String, sykmeldingId: String): String
}

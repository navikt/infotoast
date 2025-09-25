package no.nav.infotoast.sykmelder.tss

import org.springframework.stereotype.Service

@Service
class TssService(
    private val smTssClient: ISmTssClient,
) {

    fun getTssId(fnr: String, orgName: String, sykmeldingId: String): String {
        // TODO logging and error handling
        val tssIdResponse = smTssClient.getTssId(fnr, orgName, sykmeldingId)
        return tssIdResponse.getOrThrow().tssid
    }
}

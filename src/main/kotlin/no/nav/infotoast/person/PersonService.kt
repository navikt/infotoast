package no.nav.infotoast.person

import no.nav.infotoast.person.pdl.IPdlClient
import no.nav.infotoast.person.pdl.PdlPerson
import no.nav.infotoast.utils.LoggingMeta
import no.nav.infotoast.utils.logger
import no.nav.infotoast.utils.teamLogger
import org.springframework.stereotype.Service

@Service
class PersonService(
    private val pdlClient: IPdlClient,
) {
    private val logger = logger()
    private val teamLog = teamLogger()

    fun getPerson(ident: String): Person =
        pdlClient.getPerson(ident).fold(
            onSuccess = { pdlPerson ->
                Person(
                    gt = pdlPerson.gt,
                    adressebeskyttelse = pdlPerson.adressebeskyttelse,
                    sisteKontaktAdresseIUtlandet = pdlPerson.sisteKontaktAdresseIUtlandet,
                    identer = pdlPerson.identer,
                )
            },
            onFailure = { error ->
                teamLog.error("Error while fetching person info for fnr=$ident", error)
                logger.error("Error while fetching person info from PDL, check secure logs")
                throw error
            },
        )
}


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

    fun getPerson(ident: String, loggingMeta: LoggingMeta): Result<Person> {
        val person: PdlPerson =
            pdlClient.getPerson(ident).fold({ it }) {
                teamLog.error("Error while fetching person info for fnr=$ident", it)
                logger.error("Error while fetching person info from PDL, check secure logs")
                return Result.failure(it)
            }

        return Result.success(
            Person(
                gt = person.gt,
                adressebeskyttelse = person.adressebeskyttelse,
                sisteKontaktAdresseIUtlandet = person.sisteKontaktAdresseIUtlandet,
            )
        )
    }
}

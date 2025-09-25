package no.nav.infotoast.person.pdl

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local", "test")
@Component
class MockPdlClient : IPdlClient {
    override fun getPerson(fnr: String): Result<PdlPerson> {
        return Result.success(
            PdlPerson(
                gt = "Geografisk tilnærming",
                adressebeskyttelse = null,
                sisteKontaktAdresseIUtlandet = false,
            )
        )
    }
}

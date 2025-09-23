package no.nav.infotoast.tss

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local", "test")
@Component
class MockSmTssClient : ISmTssClient {

    override fun getTssId(
        fnr: String,
        orgnr: String,
        sykmeldingId: String
    ): Result<TssIdent> {
        return Result.success(
            TssIdent(
                tssid = "my-tss-id"
            )
        )
    }

}
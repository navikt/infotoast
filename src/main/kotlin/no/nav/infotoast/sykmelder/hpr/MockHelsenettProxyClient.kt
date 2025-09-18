package no.nav.infotoast.sykmelder.hpr

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local", "test")
@Component
class MockHelsenettProxyClient : IHelsenettProxyClient {
    init {
        println("MockHelsenettProxyClient initialized")
    }

    override fun getSykmelderByFnr(behandlerFnr: String, callId: String): Result<HprSykmelder> {
        if (behandlerFnr == "brokenFnr") {
            return Result.failure(
                IllegalStateException("MockHelsenettProxyClient: Simulated failure for brokenFnr")
            )
        }
        return Result.success(
            HprSykmelder(
                godkjenninger =
                    listOf(
                        HprGodkjenning(
                            helsepersonellkategori = HprKode(aktiv = true, oid = 0, verdi = "LE"),
                            autorisasjon = HprKode(aktiv = true, oid = 7704, verdi = "1"),
                        )
                    ),
                fnr = "09099012345",

            )
        )
    }
}

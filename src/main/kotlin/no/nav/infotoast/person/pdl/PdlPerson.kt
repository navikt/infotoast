package no.nav.infotoast.person.pdl

import java.time.LocalDate
import no.nav.infotoast.person.Navn
//TODO this should be the content that syfosminfotrygd receives from PDL - should the cache be updated with a new endpoint ?
data class PdlPerson(
    val navn: Navn?,
    val foedselsdato: LocalDate?,
    val identer: List<Ident>,
)

data class Ident(
    val ident: String,
    val gruppe: IDENT_GRUPPE,
    val historisk: Boolean,
)

enum class IDENT_GRUPPE {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID,
}

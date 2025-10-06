package no.nav.infotoast.person

import no.nav.infotoast.person.pdl.Ident

data class Person(
    val gt: String?,
    val adressebeskyttelse: String?,
    val sisteKontaktAdresseIUtlandet: Boolean,
    val identer: List<Ident>,
)

fun Person.getDiskresjonskode(): String? {
    return when (adressebeskyttelse) {
        "STRENGT_FORTROLIG" -> "SPSF"
        "FORTROLIG" -> "SPFO"
        else -> null
    }
}

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)

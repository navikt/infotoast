package no.nav.infotoast.person.pdl

data class PdlPerson(
    val gt: String?,
    val adressebeskyttelse: String?,
    val sisteKontaktAdresseIUtlandet: Boolean,
)

fun PdlPerson.getDiskresjonskode(): String? {
    return when (adressebeskyttelse) {
        "STRENGT_FORTROLIG" -> "SPSF"
        "FORTROLIG" -> "SPFO"
        else -> null
    }
}

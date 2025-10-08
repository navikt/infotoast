package no.nav.infotoast.person.pdl

data class PdlPerson(
    val gt: String?,
    val adressebeskyttelse: String?,
    val sisteKontaktAdresseIUtlandet: Boolean,
    val identer: List<Ident>
)

fun PdlPerson.getDiskresjonskode(): String? {
    return when (adressebeskyttelse) {
        "STRENGT_FORTROLIG" -> "SPSF"
        "FORTROLIG" -> "SPFO"
        else -> null
    }
}

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

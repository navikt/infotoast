package no.nav.infotoast.sykmelder.hpr

import no.nav.tsm.sykmelding.input.core.model.metadata.HelsepersonellKategori

data class HprSykmelder(
    val godkjenninger: List<HprGodkjenning> = emptyList(),
    val fnr: String,
)

data class HprGodkjenning(
    val helsepersonellkategori: HprKode? = null,
    val autorisasjon: HprKode? = null,
)

data class HprKode(
    val aktiv: Boolean,
    val oid: Int,
    val verdi: String?,
)


// TODO delete?
fun parseHelsepersonellKategori(v: String?): HelsepersonellKategori {
    return when (v) {
        "HE" -> HelsepersonellKategori.HELSESEKRETAR
        "KI" -> HelsepersonellKategori.KIROPRAKTOR
        "LE" -> HelsepersonellKategori.LEGE
        "MT" -> HelsepersonellKategori.MANUELLTERAPEUT
        "TL" -> HelsepersonellKategori.TANNLEGE
        "TH" -> HelsepersonellKategori.TANNHELSESEKRETAR
        "FT" -> HelsepersonellKategori.FYSIOTERAPEUT
        "SP" -> HelsepersonellKategori.SYKEPLEIER
        "HP" -> HelsepersonellKategori.HJELPEPLEIER
        "HF" -> HelsepersonellKategori.HELSEFAGARBEIDER
        "JO" -> HelsepersonellKategori.JORDMOR
        "AU" -> HelsepersonellKategori.AUDIOGRAF
        "NP" -> HelsepersonellKategori.NAPRAPAT
        "PS" -> HelsepersonellKategori.PSYKOLOG
        "FO" -> HelsepersonellKategori.FOTTERAPEUT
        "AA" -> HelsepersonellKategori.AMBULANSEARBEIDER
        "XX" -> HelsepersonellKategori.USPESIFISERT
        "HS" -> HelsepersonellKategori.UGYLDIG
        "token" -> HelsepersonellKategori.UGYLDIG
        null -> HelsepersonellKategori.IKKE_OPPGITT
        else -> throw IllegalArgumentException("Ukjent helsepersonellkategori: $v")
    }
}

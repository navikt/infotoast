package no.nav.infotoast.gcp

import java.util.zip.GZIPInputStream

fun ungzip(content: ByteArray): String {
    return GZIPInputStream(content.inputStream()).bufferedReader(Charsets.UTF_8).use {
        it.readText()
    }
}

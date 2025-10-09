package no.nav.infotoast.infotrygd

import java.io.StringWriter
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter
import no.nav.infotoast.utils.logger
import org.springframework.stereotype.Component

@Component
class InfotrygdXmlBuilder {
    private val logger = logger()

    /**
     * Builds XML message for Infotrygd from InfotrygdBlokk Returns the XML as a String ready to be
     * sent via MQ
     */
    fun buildXmlMessage(blokk: InfotrygdBlokk): String {
        logger.info("Building Infotrygd XML message for sykmelding ${blokk.sykmeldingId}")

        val stringWriter = StringWriter()
        val xmlFactory = XMLOutputFactory.newInstance()
        val writer = xmlFactory.createXMLStreamWriter(stringWriter)

        writer.writeStartDocument("UTF-8", "1.0")
        writer.writeStartElement("InfotrygdOppdatering")

        // Header section
        writeHeader(writer, blokk)

        // Patient section
        writePatient(writer, blokk)

        // Behandler (doctor) section
        writeBehandler(writer, blokk)

        // Diagnose section
        writeDiagnose(writer, blokk)

        // Perioder section
        writePerioder(writer, blokk)

        // Arbeidsgiver section (if present)
        if (blokk.arbeidsgiver != null) {
            writeArbeidsgiver(writer, blokk.arbeidsgiver)
        }

        writer.writeEndElement() // Close InfotrygdOppdatering
        writer.writeEndDocument()
        writer.close()

        val xml = stringWriter.toString()
        logger.info("Successfully built Infotrygd XML message for sykmelding ${blokk.sykmeldingId}")
        return xml
    }

    private fun writeHeader(writer: XMLStreamWriter, blokk: InfotrygdBlokk) {
        writer.writeStartElement("Header")
        writeElement(writer, "SykmeldingId", blokk.sykmeldingId)
        writeElement(writer, "JournalpostId", blokk.journalpostId)
        writeElement(writer, "NavKontorNr", blokk.navKontorNr)
        writeElement(writer, "SignaturDato", blokk.signaturDato.toString())
        blokk.forsteFravaersdag?.let { writeElement(writer, "ForsteFravaersdag", it.toString()) }
        writer.writeEndElement()
    }

    private fun writePatient(writer: XMLStreamWriter, blokk: InfotrygdBlokk) {
        writer.writeStartElement("Pasient")
        writeElement(writer, "Fnr", blokk.pasientFnr)
        writer.writeEndElement()
    }

    private fun writeBehandler(writer: XMLStreamWriter, blokk: InfotrygdBlokk) {
        writer.writeStartElement("Behandler")
        writeElement(writer, "Fnr", blokk.behandlerFnr)
        blokk.behandlerHpr?.let { writeElement(writer, "HprNummer", it) }
        writeElement(writer, "Kategori", blokk.helsepersonellKategori)
        writeElement(writer, "TssId", blokk.tssId)
        writer.writeEndElement()
    }

    private fun writeDiagnose(writer: XMLStreamWriter, blokk: InfotrygdBlokk) {
        writer.writeStartElement("Diagnose")

        blokk.diagnose.hovedDiagnose?.let { diagnose ->
            writer.writeStartElement("HovedDiagnose")
            writeElement(writer, "Kode", diagnose.kode)
            writeElement(writer, "Kodeverk", diagnose.kodeverk)
            diagnose.tekst?.let { writeElement(writer, "Tekst", it) }
            writer.writeEndElement()
        }

        if (blokk.diagnose.biDiagnoser.isNotEmpty()) {
            writer.writeStartElement("BiDiagnoser")
            blokk.diagnose.biDiagnoser.forEach { diagnose ->
                writer.writeStartElement("Diagnose")
                writeElement(writer, "Kode", diagnose.kode)
                writeElement(writer, "Kodeverk", diagnose.kodeverk)
                diagnose.tekst?.let { writeElement(writer, "Tekst", it) }
                writer.writeEndElement()
            }
            writer.writeEndElement()
        }

        writer.writeEndElement()
    }

    private fun writePerioder(writer: XMLStreamWriter, blokk: InfotrygdBlokk) {
        writer.writeStartElement("Perioder")

        blokk.perioder.forEach { periode ->
            writer.writeStartElement("Periode")
            writeElement(writer, "Fom", periode.fom.toString())
            writeElement(writer, "Tom", periode.tom.toString())
            periode.grad?.let { writeElement(writer, "Grad", it.toString()) }
            writeElement(writer, "Type", periode.typeSykmelding)
            writer.writeEndElement()
        }

        writer.writeEndElement()
    }

    private fun writeArbeidsgiver(writer: XMLStreamWriter, arbeidsgiver: InfotrygdArbeidsgiver) {
        writer.writeStartElement("Arbeidsgiver")
        arbeidsgiver.navn?.let { writeElement(writer, "Navn", it) }
        arbeidsgiver.orgnummer?.let { writeElement(writer, "Orgnummer", it) }
        writer.writeEndElement()
    }

    private fun writeElement(writer: XMLStreamWriter, name: String, value: String) {
        writer.writeStartElement(name)
        writer.writeCharacters(value)
        writer.writeEndElement()
    }
}

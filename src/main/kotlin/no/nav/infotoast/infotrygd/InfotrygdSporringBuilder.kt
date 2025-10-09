package no.nav.infotoast.infotrygd

import java.io.StringWriter
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter
import no.nav.infotoast.utils.logger
import org.springframework.stereotype.Component

@Component
class InfotrygdSporringBuilder {
    private val logger = logger()

    /**
     * Builds an Infotrygd query (sporring/forespørsel) XML message This is sent before the update
     * to check existing data in Infotrygd
     */
    fun buildSporringMessage(
        pasientFnr: String,
        behandlerFnr: String,
        tssId: String,
        sykmeldingId: String,
        hovedDiagnoseKode: String?,
        hovedDiagnoseKodeverk: String?,
        biDiagnoseKode: String?,
        biDiagnoseKodeverk: String?,
    ): String {
        logger.info("Building Infotrygd sporring message for sykmelding $sykmeldingId")

        val stringWriter = StringWriter()
        val xmlFactory = XMLOutputFactory.newInstance()
        val writer = xmlFactory.createXMLStreamWriter(stringWriter)

        writer.writeStartDocument("UTF-8", "1.0")
        writer.writeStartElement("InfotrygdSporring")

        // Pasient section
        writer.writeStartElement("Pasient")
        writeElement(writer, "Fnr", pasientFnr)
        writer.writeEndElement()

        // Behandler section
        writer.writeStartElement("Behandler")
        writeElement(writer, "Fnr", behandlerFnr)
        writeElement(writer, "TssId", tssId)
        writer.writeEndElement()

        // Diagnose section (for matching existing sykmeldinger)
        if (hovedDiagnoseKode != null && hovedDiagnoseKodeverk != null) {
            writer.writeStartElement("Diagnose")
            writer.writeStartElement("HovedDiagnose")
            writeElement(writer, "Kode", hovedDiagnoseKode)
            writeElement(writer, "Kodeverk", hovedDiagnoseKodeverk)
            writer.writeEndElement()

            if (biDiagnoseKode != null && biDiagnoseKodeverk != null) {
                writer.writeStartElement("BiDiagnose")
                writeElement(writer, "Kode", biDiagnoseKode)
                writeElement(writer, "Kodeverk", biDiagnoseKodeverk)
                writer.writeEndElement()
            }

            writer.writeEndElement() // Close Diagnose
        }

        writer.writeEndElement() // Close InfotrygdSporring
        writer.writeEndDocument()
        writer.close()

        val xml = stringWriter.toString()
        logger.info("Successfully built Infotrygd sporring message for sykmelding $sykmeldingId")
        return xml
    }

    private fun writeElement(writer: XMLStreamWriter, name: String, value: String) {
        writer.writeStartElement(name)
        writer.writeCharacters(value)
        writer.writeEndElement()
    }
}

package no.nav.infotoast

import no.nav.infotoast.gcp.GcpBucketService
import no.nav.infotoast.infotrygd.mq.InfotrygdMqService
import no.nav.infotoast.person.Person
import no.nav.infotoast.utils.logger
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.springframework.stereotype.Service

@Service
class InfotrygdService(
    private val infotrygdMqService: InfotrygdMqService,
    private val gcpBucketService: GcpBucketService
) {
    private val logger = logger()

    fun updateInfotrygd(
        tssId: String,
        sykmeldingRecord: SykmeldingRecord,
        journalpostId: String,
        pdlPerson: Person
    ) {
        val sykmeldingId = sykmeldingRecord.sykmelding.id
        logger.info("Preparing to update Infotrygd for sykmelding $sykmeldingId")

        // Retrieve the fellesformat from GCP bucket
        val fellesformat = gcpBucketService.getFellesformat(sykmeldingId)

        if (fellesformat == null) {
            logger.error("Could not retrieve fellesformat for sykmeldingId $sykmeldingId")
            throw IllegalStateException("Fellesformat not found for sykmeldingId $sykmeldingId")
        }

        // TODO: We need to use the fellesformat to create the complete Infotrygd update message
        // TODO: Extract necessary information from fellesformat
        // TODO: Create XML message from fellesformat and send via MQ
        // val xmlMessage = createInfotrygdXmlMessage(fellesformat, tssId, journalpostId)
        // infotrygdMqService.sendInfotrygdOppdatering(xmlMessage)

        logger.info("Infotrygd update request prepared for sykmelding $sykmeldingId")
        logger.info(
            "Fellesformat retrieved, waiting for Infotrygd XML message creation implementation"
        )
    }
}

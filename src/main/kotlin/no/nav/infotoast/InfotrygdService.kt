package no.nav.infotoast
//
//import no.nav.infotoast.fellesformat.FellesformatExtractorService
//import no.nav.infotoast.gcp.IGcpBucketService
//import no.nav.infotoast.infotrygd.InfotrygdBlokkBuilder
//import no.nav.infotoast.infotrygd.InfotrygdProcessingState
//import no.nav.infotoast.infotrygd.InfotrygdProcessingStateService
//import no.nav.infotoast.infotrygd.InfotrygdSporringBuilder
//import no.nav.infotoast.infotrygd.InfotrygdXmlBuilder
//import no.nav.infotoast.infotrygd.ProcessingStep
//import no.nav.infotoast.infotrygd.mq.IInfotrygdMqService
//import no.nav.infotoast.person.Person
//import no.nav.infotoast.utils.logger
//import no.nav.infotoast.utils.teamLogger
//import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
//import org.springframework.stereotype.Service
//
////@Service
//class InfotrygdService(
//    private val infotrygdMqService: IInfotrygdMqService,
//    private val gcpBucketService: IGcpBucketService,
//    private val fellesformatExtractor: FellesformatExtractorService,
//    private val infotrygdBlokkBuilder: InfotrygdBlokkBuilder,
//    private val infotrygdXmlBuilder: InfotrygdXmlBuilder,
//    private val infotrygdSporringBuilder: InfotrygdSporringBuilder,
//    private val processingStateService: InfotrygdProcessingStateService,
//) {
//    private val logger = logger()
//    private val teamLogger = teamLogger()
//
//    /**
//     * Initiates async Infotrygd processing by sending sporring query The response will be handled
//     * asynchronously by InfotrygdResponseListener
//     */
//    fun initiateInfotrygdProcessing(
//        tssId: String,
//        sykmeldingRecord: SykmeldingRecord,
//        journalpostId: String,
//        pdlPerson: Person,
//        helsepersonellKategori: String,
//        navKontorNr: String,
//    ) {
//        val sykmeldingId = sykmeldingRecord.sykmelding.id
//        logger.info("Initiating async Infotrygd processing for sykmelding $sykmeldingId")
//
//        try {
//            // Retrieve the fellesformat from GCP bucket
//            val fellesformat = gcpBucketService.getFellesformat(sykmeldingId)
//
//            if (fellesformat == null) {
//                logger.error("Could not retrieve fellesformat for sykmeldingId $sykmeldingId")
//                throw IllegalStateException("Fellesformat not found for sykmeldingId $sykmeldingId")
//            }
//
//            // Extract all health information from fellesformat
//            val healthInfo = fellesformatExtractor.extractHealthInfo(fellesformat)
//            logger.info(
//                "Extracted health info for sykmelding $sykmeldingId: hovedDiagnose=${healthInfo.hovedDiagnose?.kode}, perioder=${healthInfo.perioder.size}"
//            )
//
//            val behandlerFnr =
//                healthInfo.behandlerFnr ?: throw IllegalStateException("Behandler FNR is required")
//
//            // Create initial processing state in Valkey
//            val initialState =
//                InfotrygdProcessingState(
//                    sykmeldingId = sykmeldingId,
//                    journalpostId = journalpostId,
//                    tssId = tssId,
//                    helsepersonellKategori = helsepersonellKategori,
//                    navKontorNr = navKontorNr,
//                    pasientFnr = healthInfo.pasientFnr,
//                    behandlerFnr = behandlerFnr,
//                    currentStep = ProcessingStep.INITIATED
//                )
//            processingStateService.saveState(initialState)
//
//            // Build and send sporring (query) to Infotrygd
//            val sporringXml =
//                infotrygdSporringBuilder.buildSporringMessage(
//                    pasientFnr = healthInfo.pasientFnr,
//                    behandlerFnr = behandlerFnr,
//                    tssId = tssId,
//                    sykmeldingId = sykmeldingId,
//                    hovedDiagnoseKode = healthInfo.hovedDiagnose?.kode,
//                    hovedDiagnoseKodeverk =
//                        healthInfo.hovedDiagnose?.system?.let {
//                            no.nav.infotoast.infotrygd.mapDiagnoseKodeverk(it)
//                        },
//                    biDiagnoseKode = healthInfo.biDiagnoser.firstOrNull()?.kode,
//                    biDiagnoseKodeverk =
//                        healthInfo.biDiagnoser.firstOrNull()?.system?.let {
//                            no.nav.infotoast.infotrygd.mapDiagnoseKodeverk(it)
//                        },
//                )
//
//            val sporringCorrelationId =
//                infotrygdMqService.sendInfotrygdSporring(sporringXml, sykmeldingId)
//
//            // Update state to SPORRING_SENT with correlation ID
//            processingStateService.updateStep(
//                sykmeldingId = sykmeldingId,
//                step = ProcessingStep.SPORRING_SENT,
//                correlationId = sporringCorrelationId
//            )
//
//            logger.info(
//                "Sent Infotrygd sporring for sykmelding $sykmeldingId with correlationId $sporringCorrelationId. Async response will trigger oppdatering."
//            )
//        } catch (e: Exception) {
//            logger.error("Failed to initiate Infotrygd processing for sykmelding $sykmeldingId", e)
//            processingStateService.markFailed(sykmeldingId, e.message ?: "Unknown error")
//            throw e
//        }
//    }
//
//    /**
//     * Sends oppdatering to Infotrygd (called by response listener after sporring response) This is
//     * the second step in the async flow
//     *
//     * NOTE: Oppdatering is FIRE-AND-FORGET. We send it and immediately mark as completed. We do NOT
//     * wait for or handle any response from this message.
//     */
//    fun sendInfotrygdOppdatering(sykmeldingId: String) {
//        logger.info("Sending Infotrygd oppdatering for sykmelding $sykmeldingId (fire-and-forget)")
//
//        try {
//            val state =
//                processingStateService.getState(sykmeldingId)
//                    ?: throw IllegalStateException(
//                        "Processing state not found for sykmelding $sykmeldingId"
//                    )
//
//            if (state.currentStep != ProcessingStep.SPORRING_RECEIVED) {
//                logger.error(
//                    "Invalid state for oppdatering: sykmelding $sykmeldingId is in step ${state.currentStep}, expected SPORRING_RECEIVED"
//                )
//                throw IllegalStateException("Invalid processing step for oppdatering")
//            }
//
//            // Retrieve fellesformat again
//            val fellesformat =
//                gcpBucketService.getFellesformat(sykmeldingId)
//                    ?: throw IllegalStateException(
//                        "Fellesformat not found for sykmeldingId $sykmeldingId"
//                    )
//
//            val healthInfo = fellesformatExtractor.extractHealthInfo(fellesformat)
//
//            // Build InfotrygdBlokk with all collected data (including identDato and tkNummer from
//            // sporring)
//            val infotrygdBlokk =
//                infotrygdBlokkBuilder.buildInfotrygdBlokk(
//                    healthInfo = healthInfo,
//                    tssId = state.tssId,
//                    journalpostId = state.journalpostId,
//                    navKontorNr = state.navKontorNr,
//                    helsepersonellKategori = state.helsepersonellKategori,
//                )
//            logger.info("Built InfotrygdBlokk for sykmelding $sykmeldingId")
//            teamLogger.info(
//                "Built InfotrygdBlokk for sykmelding $sykmeldingId with data: $infotrygdBlokk"
//            )
//
//            // Generate XML message for oppdatering
//            val oppdateringXml = infotrygdXmlBuilder.buildXmlMessage(infotrygdBlokk)
//            logger.info(
//                "Generated XML message for sykmelding $sykmeldingId, length=${oppdateringXml.length}"
//            )
//
//            // Update state to OPPDATERING_SENT (no correlation ID needed since we don't expect a
//            // response)
//            processingStateService.updateStep(
//                sykmeldingId = sykmeldingId,
//                step = ProcessingStep.OPPDATERING_SENT
//            )
//
//            // Send oppdatering to Infotrygd via MQ (fire-and-forget)
//            infotrygdMqService.sendInfotrygdOppdatering(oppdateringXml, sykmeldingId)
//
//            // Immediately mark as COMPLETED since we don't wait for a response
//            processingStateService.updateStep(sykmeldingId, ProcessingStep.COMPLETED)
//
//            logger.info(
//                "Sent Infotrygd oppdatering for sykmelding $sykmeldingId and marked as COMPLETED (fire-and-forget)"
//            )
//        } catch (e: Exception) {
//            logger.error("Failed to send Infotrygd oppdatering for sykmelding $sykmeldingId", e)
//            processingStateService.markFailed(sykmeldingId, e.message ?: "Unknown error")
//            throw e
//        }
//    }
//
//    /** Retry sporring for a failed message (called by retry scheduler) */
//    fun retrySporring(state: InfotrygdProcessingState) {
//        val sykmeldingId = state.sykmeldingId
//        logger.info(
//            "Retrying sporring for sykmelding $sykmeldingId (retry ${state.retryCount + 1})"
//        )
//
//        try {
//            // Retrieve fellesformat
//            val fellesformat =
//                gcpBucketService.getFellesformat(sykmeldingId)
//                    ?: throw IllegalStateException(
//                        "Fellesformat not found for sykmeldingId $sykmeldingId"
//                    )
//
//            val healthInfo = fellesformatExtractor.extractHealthInfo(fellesformat)
//
//            // Build and send sporring
//            val sporringXml =
//                infotrygdSporringBuilder.buildSporringMessage(
//                    pasientFnr = state.pasientFnr,
//                    behandlerFnr = state.behandlerFnr,
//                    tssId = state.tssId,
//                    sykmeldingId = sykmeldingId,
//                    hovedDiagnoseKode = healthInfo.hovedDiagnose?.kode,
//                    hovedDiagnoseKodeverk =
//                        healthInfo.hovedDiagnose?.system?.let {
//                            no.nav.infotoast.infotrygd.mapDiagnoseKodeverk(it)
//                        },
//                    biDiagnoseKode = healthInfo.biDiagnoser.firstOrNull()?.kode,
//                    biDiagnoseKodeverk =
//                        healthInfo.biDiagnoser.firstOrNull()?.system?.let {
//                            no.nav.infotoast.infotrygd.mapDiagnoseKodeverk(it)
//                        },
//                )
//
//            val sporringCorrelationId =
//                infotrygdMqService.sendInfotrygdSporring(sporringXml, sykmeldingId)
//
//            // Update state to SPORRING_SENT
//            processingStateService.updateStep(
//                sykmeldingId = sykmeldingId,
//                step = ProcessingStep.SPORRING_SENT,
//                correlationId = sporringCorrelationId
//            )
//
//            logger.info(
//                "Retry: Sent sporring for sykmelding $sykmeldingId with correlationId $sporringCorrelationId"
//            )
//        } catch (e: Exception) {
//            logger.error("Failed to retry sporring for sykmelding $sykmeldingId", e)
//            processingStateService.markFailed(sykmeldingId, "Retry failed: ${e.message}")
//            throw e
//        }
//    }
//}

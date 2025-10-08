package no.nav.infotoast.gcp

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.Storage
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.infotoast.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class GcpBucketService(
    @Value("\${gcp.bucket.name}") private val bucketName: String,
    private val storage: Storage,
    private val xmlHandler: XmlHandler,
) : IGcpBucketService {
    private val logger = logger()

    /**
     * Downloads the fellesformat XML for a sykmelding from GCP bucket
     *
     * @param sykmeldingId the ID of the sykmelding
     * @return XMLEIFellesformat if found, null otherwise
     */
    override fun getFellesformat(sykmeldingId: String): XMLEIFellesformat? {
        logger.info(
            "Attempting to download fellesformat for sykmeldingId: $sykmeldingId from bucket: $bucketName"
        )

        return try {
            val blobId = BlobId.of(bucketName, sykmeldingId)
            val blob = storage.get(blobId)

            if (blob != null && blob.exists()) {
                val compressedData = blob.getContent()
                logger.info(
                    "Downloaded ${compressedData.size} bytes for sykmeldingId: $sykmeldingId from bucket $bucketName"
                )

                // Decompress the gzipped content
                val decompressed = ungzip(compressedData)

                // Unmarshal XML to fellesformat object
                val fellesformat = xmlHandler.unmarshal(decompressed)
                logger.info(
                    "Successfully unmarshalled fellesformat for sykmeldingId: $sykmeldingId"
                )

                fellesformat
            } else {
                logger.warn("Fellesformat not found in bucket for sykmeldingId: $sykmeldingId")
                null
            }
        } catch (e: Exception) {
            logger.error("Error downloading fellesformat for sykmeldingId: $sykmeldingId", e)
            throw e
        }
    }

    /** Checks if a fellesformat exists in the bucket for the given sykmeldingId */
    override fun fellesformatExists(sykmeldingId: String): Boolean {
        return try {
            val blobId = BlobId.of(bucketName, sykmeldingId)
            val blob = storage.get(blobId)
            blob != null && blob.exists()
        } catch (e: Exception) {
            logger.error("Error checking if fellesformat exists for sykmeldingId: $sykmeldingId", e)
            false
        }
    }
}

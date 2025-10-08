package no.nav.infotoast.mq

import java.util.Properties

class MqTlsUtils {
    companion object {
        fun getMqTlsConfig(): Properties {
            val trustStorePath =
                System.getenv("NAV_TRUSTSTORE_PATH")
                    ?: throw RuntimeException("Missing required variable \"NAV_TRUSTSTORE_PATH\"")
            val trustStorePassword =
                System.getenv("NAV_TRUSTSTORE_PASSWORD")
                    ?: throw RuntimeException(
                        "Missing required variable \"NAV_TRUSTSTORE_PASSWORD\""
                    )

            return Properties().also {
                it["javax.net.ssl.keyStore"] = trustStorePath
                it["javax.net.ssl.keyStorePassword"] = trustStorePassword
                it["javax.net.ssl.keyStoreType"] = "jks"
            }
        }
    }
}

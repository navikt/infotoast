package no.nav.infotoast.mq

import jakarta.jms.Connection
import no.nav.infotoast.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MqConfiguration(
    @Value("\${mq.hostname}") private val mqHostname: String,
    @Value("\${mq.port}") private val mqPort: Int,
    @Value("\${mq.gateway.name}") private val mqGatewayName: String,
    @Value("\${mq.channel.name}") private val mqChannelName: String,
    @Value("\${mq.username}") private val mqUsername: String,
    @Value("\${mq.password}") private val mqPassword: String,
) {
    private val logger = logger()

    @Bean
    fun mqConfig(): MqConfig =
        object : MqConfig {
            override val mqHostname: String = this@MqConfiguration.mqHostname
            override val mqPort: Int = this@MqConfiguration.mqPort
            override val mqGatewayName: String = this@MqConfiguration.mqGatewayName
            override val mqChannelName: String = this@MqConfiguration.mqChannelName
        }

    @Bean
    fun mqConnection(mqConfig: MqConfig): Connection {
        // Set up TLS configuration
        MqTlsUtils.getMqTlsConfig().forEach { key, value ->
            System.setProperty(key as String, value as String)
        }

        logger.info(
            "Connecting to MQ: hostname=$mqHostname, port=$mqPort, queueManager=$mqGatewayName, channel=$mqChannelName"
        )

        val factory = connectionFactory(mqConfig)
        val connection = factory.createConnection(mqUsername, mqPassword)
        connection.start()

        logger.info("Successfully connected to MQ")
        return connection
    }
}

package no.nav.infotoast.mq

interface MqConfig {
    val mqHostname: String
    val mqPort: Int
    val mqGatewayName: String
    val mqChannelName: String
}

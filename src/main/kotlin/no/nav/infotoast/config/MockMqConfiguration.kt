package no.nav.infotoast.mq

import jakarta.jms.*
import no.nav.infotoast.utils.logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("local", "test")
@Configuration
class MockMqConfiguration {
    private val logger = logger()

    @Bean
    fun mqConnection(): Connection {
        logger.info("MOCK: Creating mock MQ Connection for local/test profile")
        return MockMqConnection()
    }
}

/** Simple mock MQ Connection that does nothing - used for local/test profiles */
private class MockMqConnection : Connection {
    override fun createSession(transacted: Boolean, acknowledgeMode: Int): Session {
        return MockSession()
    }

    override fun createSession(acknowledgeMode: Int): Session {
        return MockSession()
    }

    override fun createSession(): Session {
        return MockSession()
    }

    override fun getClientID(): String = "mock-client-id"

    override fun setClientID(clientID: String?) {}

    override fun getMetaData(): ConnectionMetaData? = null

    override fun getExceptionListener(): ExceptionListener? = null

    override fun setExceptionListener(listener: ExceptionListener?) {}

    override fun start() {}

    override fun stop() {}

    override fun close() {}

    override fun createConnectionConsumer(
        destination: Destination?,
        messageSelector: String?,
        sessionPool: ServerSessionPool?,
        maxMessages: Int
    ): ConnectionConsumer? = null

    override fun createSharedConnectionConsumer(
        topic: Topic?,
        subscriptionName: String?,
        messageSelector: String?,
        sessionPool: ServerSessionPool?,
        maxMessages: Int
    ): ConnectionConsumer? = null

    override fun createDurableConnectionConsumer(
        topic: Topic?,
        subscriptionName: String?,
        messageSelector: String?,
        sessionPool: ServerSessionPool?,
        maxMessages: Int
    ): ConnectionConsumer? = null

    override fun createSharedDurableConnectionConsumer(
        topic: Topic?,
        subscriptionName: String?,
        messageSelector: String?,
        sessionPool: ServerSessionPool?,
        maxMessages: Int
    ): ConnectionConsumer? = null
}

/** Simple mock Session that does nothing */
private class MockSession : Session {
    override fun close() {}

    override fun createQueue(queueName: String?): Queue? = null

    override fun createTopic(topicName: String?): Topic? = null

    override fun createConsumer(destination: Destination?): MessageConsumer? = null

    override fun createConsumer(
        destination: Destination?,
        messageSelector: String?
    ): MessageConsumer? = null

    override fun createConsumer(
        destination: Destination?,
        messageSelector: String?,
        noLocal: Boolean
    ): MessageConsumer? = null

    override fun createProducer(destination: Destination?): MessageProducer? = null

    override fun createTextMessage(): TextMessage? = null

    override fun createTextMessage(text: String?): TextMessage? = null

    override fun createBytesMessage(): BytesMessage? = null

    override fun createMapMessage(): MapMessage? = null

    override fun createMessage(): Message? = null

    override fun createObjectMessage(): ObjectMessage? = null

    override fun createObjectMessage(obj: java.io.Serializable?): ObjectMessage? = null

    override fun createStreamMessage(): StreamMessage? = null

    override fun getTransacted(): Boolean = false

    override fun getAcknowledgeMode(): Int = Session.AUTO_ACKNOWLEDGE

    override fun commit() {}

    override fun rollback() {}

    override fun recover() {}

    override fun getMessageListener(): MessageListener? = null

    override fun setMessageListener(listener: MessageListener?) {}

    override fun run() {}

    override fun createBrowser(queue: Queue?): QueueBrowser? = null

    override fun createBrowser(queue: Queue?, messageSelector: String?): QueueBrowser? = null

    override fun createTemporaryQueue(): TemporaryQueue? = null

    override fun createTemporaryTopic(): TemporaryTopic? = null

    override fun unsubscribe(name: String?) {}

    override fun createDurableConsumer(topic: Topic?, name: String?): MessageConsumer? = null

    override fun createDurableConsumer(
        topic: Topic?,
        name: String?,
        messageSelector: String?,
        noLocal: Boolean
    ): MessageConsumer? = null

    override fun createDurableSubscriber(topic: Topic?, name: String?): TopicSubscriber? = null

    override fun createDurableSubscriber(
        topic: Topic?,
        name: String?,
        messageSelector: String?,
        noLocal: Boolean
    ): TopicSubscriber? = null

    override fun createSharedConsumer(
        topic: Topic?,
        sharedSubscriptionName: String?
    ): MessageConsumer? = null

    override fun createSharedConsumer(
        topic: Topic?,
        sharedSubscriptionName: String?,
        messageSelector: String?
    ): MessageConsumer? = null

    override fun createSharedDurableConsumer(topic: Topic?, name: String?): MessageConsumer? = null

    override fun createSharedDurableConsumer(
        topic: Topic?,
        name: String?,
        messageSelector: String?
    ): MessageConsumer? = null
}

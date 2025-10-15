package no.nav.infotoast.oppgave.kafka.producer

import no.nav.infotoast.oppgave.kafka.OppgaveRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafka

@Configuration
@EnableKafka
@EnableConfigurationProperties
class ProducerConfig {

    @Bean fun oppgaveTopic(@Value("\${kafka.topics.oppgave}") oppgaveTopic: String) = oppgaveTopic

    @Bean
    @Profile("!local & !dev-kafka & !test")
    @Qualifier("oppgaveKafkaProducer")
    fun oppgaveKafkaProducer(props: KafkaProperties): KafkaProducer<String, OppgaveRecord> {
        val producerProps = props.buildProducerProperties(null)
        // Ensure serializers are correct
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] =
            OppgaveRecordSerializer::class.java
        return KafkaProducer(producerProps)
    }

    @Bean
    @Profile("!local & !dev-kafka & !test")
    fun oppgaveProducer(
        @Qualifier("oppgaveKafkaProducer") kafkaProducer: KafkaProducer<String, OppgaveRecord>,
        @Value("\${kafka.topics.oppgave}") topic: String,
    ): OppgaveProducer = OppgaveProducer(kafkaProducer, topic)

    // For local/dev/test we use a simpler configuration
    @Bean
    @Profile("local | dev-kafka | test")
    @Qualifier("oppgaveKafkaProducer")
    fun oppgaveKafkaProducerLocal(props: KafkaProperties): KafkaProducer<String, OppgaveRecord> {
        val producerProps = props.buildProducerProperties(null)
        producerProps[
            org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] =
            StringSerializer::class.java
        producerProps[
            org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] =
            OppgaveRecordSerializer::class.java
        return KafkaProducer(producerProps)
    }

    @Bean
    @Profile("local | dev-kafka | test")
    fun oppgaveProducerLocal(
        @Qualifier("oppgaveKafkaProducer") kafkaProducer: KafkaProducer<String, OppgaveRecord>,
        @Value("\${kafka.topics.oppgave}") topic: String,
    ): OppgaveProducer = OppgaveProducer(kafkaProducer, topic)
}

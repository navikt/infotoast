package no.nav.infotoast.sykmelding.kafka.producer

import no.nav.infotoast.oppgave.OppgaveRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer
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
    fun oppgaveKafkaProducer(props: KafkaProperties): KafkaProducer<String, OppgaveRecord> {
        val producerProps = props.buildProducerProperties(null)
        // Ensure serializers are correct
        producerProps[
            org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] =
            StringSerializer::class.java
        producerProps[
            org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] =
            OppgaveRecordSerializer::class.java
        return KafkaProducer(producerProps)
    }

    @Bean
    fun oppgaveProducer(
        kafkaProducer: KafkaProducer<String, OppgaveRecord>,
        @Value("\${kafka.topics.oppgave}") topic: String,
    ): OppgaveProducer = OppgaveProducer(kafkaProducer, topic)

    // For local/dev/test we still use the same configuration provided by Spring Boot Kafka
    @Bean
    @Profile("local", "dev-kafka", "test")
    fun oppgaveKafkaProducerLocal(props: KafkaProperties): KafkaProducer<String, OppgaveRecord> =
        oppgaveKafkaProducer(props)
}

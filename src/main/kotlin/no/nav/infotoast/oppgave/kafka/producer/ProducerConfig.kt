package no.nav.infotoast.sykmelding.kafka.producer

import no.nav.infotoast.oppgave.OppgaveRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProducerConfig {

    @Bean fun oppgaveTopic(@Value("\${kafka.topics.oppgave}") oppgaveTopic: String) = oppgaveTopic

    @Bean
    fun oppgaveKafkaProducer(props: KafkaProperties): KafkaProducer<String, OppgaveRecord> {
        val producerProps = props.buildProducerProperties(null).toMutableMap()

        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = OppgaveRecordSerializer::class.java.name

        return KafkaProducer(producerProps)
    }

    @Bean
    fun oppgaveProducer(
        oppgaveKafkaProducer: KafkaProducer<String, OppgaveRecord>,
        @Value("\${kafka.topics.oppgave}") topic: String,
    ): OppgaveProducer = OppgaveProducer(oppgaveKafkaProducer, topic)
}

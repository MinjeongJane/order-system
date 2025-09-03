package order.common.config

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class KafkaProducerConfig {
    @Bean
    fun <T : Any> producerFactory(): ProducerFactory<String, T> {
        val props = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9093",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            // 데이터 손실 최소화를 위해 모든 ISR(Replica)이 받을 때까지 대기
            ProducerConfig.ACKS_CONFIG to "all",
            // 실패 시 재시도 횟수
            ProducerConfig.RETRIES_CONFIG to 3,
            // 중복 방지를 위한 idempotent 프로듀서 활성화 (optional)
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true
        )
        return DefaultKafkaProducerFactory(props)
    }

    @Bean
    fun <T : Any> kafkaTemplate(): KafkaTemplate<String, T> =
        KafkaTemplate(producerFactory())
}
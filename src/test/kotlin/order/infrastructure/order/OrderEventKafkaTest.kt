package order.infrastructure.order

import order.common.config.JacksonConfig
import order.domain.event.OrderDetailsEvent
import order.domain.event.OrderHistoryEvent
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.LocalDateTime
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = ["order-events"])
@TestPropertySource(properties = ["spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}"])
@Import(JacksonConfig::class)
class OrderEventKafkaTest(
    @Autowired val embeddedKafka: EmbeddedKafkaBroker,
) {
    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `이벤트가 토픽에 정상적으로 전송되는지 확인`() {
        // given
        val event = OrderHistoryEvent(1L, 2L, 1000, listOf(OrderDetailsEvent(1, 2)), LocalDateTime.now())

        val producerProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to embeddedKafka.brokersAsString,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java
        )
        val producerFactory = DefaultKafkaProducerFactory<String, OrderHistoryEvent>(producerProps)
        val kafkaTemplate = KafkaTemplate(producerFactory)

        val consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafka).apply {
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        }

        val valueDeserializer = JsonDeserializer(OrderHistoryEvent::class.java, objectMapper, false).apply {
            addTrustedPackages("*")
        }

        val consumerFactory = DefaultKafkaConsumerFactory(
            consumerProps,
            StringDeserializer(),
            valueDeserializer
        )
        val consumer = consumerFactory.createConsumer()
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "order-events")

        // when
        kafkaTemplate.send("order-events", event).get()
        kafkaTemplate.flush()

        // then
        val record = KafkaTestUtils.getSingleRecord(consumer, "order-events", Duration.ofSeconds(10))
        assertEquals(event.orderId, record.value().orderId)
    }
}
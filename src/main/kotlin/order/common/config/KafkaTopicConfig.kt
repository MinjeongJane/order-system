package order.common.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig {
    @Bean
    fun orderEventsTopic(): NewTopic =
        TopicBuilder.name("order-events")
            .partitions(3)
            .replicas(1)
            .build()
}
package order

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
class OrderSystemApplication

fun main(args: Array<String>) {
    runApplication<OrderSystemApplication>(*args)
}
package order.common.config

import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Neo4jConfig(
    @Value("\${neo4j.uri}") private val uri: String,
    @Value("\${neo4j.username}") private val username: String,
    @Value("\${neo4j.password}") private val password: String,
) {
    /**
     * Neo4j Driver 는 애플리케이션당 하나만 두고 재사용한다.
     * 내부에 커넥션 풀을 들고 있어 (HikariDataSource 처럼) 매번 생성하면 안 된다.
     */
    @Bean(destroyMethod = "close")
    fun neo4jDriver(): Driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password))
}

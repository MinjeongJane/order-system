plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.3"
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.spring") version "2.1.10"
    kotlin("kapt") version "2.1.10"
    kotlin("plugin.jpa") version "2.1.10"
    jacoco

    scala
    id("io.gatling.gradle") version "3.10.5"
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

group = "order-system"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")

    implementation("com.mysql:mysql-connector-j")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    kapt("jakarta.annotation:jakarta.annotation-api:2.1.1")
    kapt("jakarta.persistence:jakarta.persistence-api:3.0.0")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.redisson:redisson-spring-boot-starter:3.23.5")

    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.1.2")

    // prometheus
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // gatling
    implementation("io.gatling:gatling-core:3.10.5")
    implementation("io.gatling:gatling-http:3.10.5")

    // resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.6.1")
    testImplementation("io.kotest:kotest-assertions-core:5.6.1")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.3")
    testImplementation("org.springframework.kafka:spring-kafka-test")

    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

sourceSets {
    main {
        java {
            srcDir("build/generated/kapt/main")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

tasks.register("composeUp") {
    doLast {
        exec {
            commandLine("docker", "compose", "up", "-d")
        }
    }
}

tasks.named("test") {
    dependsOn("composeUp")
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) {
            exclude(
                "**/Q*.class",
                "**/*ApplicationKt.class",
                "**/*Application.class",
                // JPA Entities (infrastructure layer, tested via integration tests)
                "**/infrastructure/**/*Entity.class",
                "**/infrastructure/common/BaseEntity.class",
                // JPA Repositories (interfaces)
                "**/*JpaRepository.class",
                // Pure config classes with no business logic
                "**/config/AsyncConfig.class",
                "**/config/AuditorAwareConfig.class",
                "**/config/JpaConfig.class",
                "**/config/KafkaProducerConfig.class",
                "**/config/KafkaTopicConfig.class",
                "**/config/OpenAiFeignConfig.class",
                "**/config/QueryDslConfig.class",
                "**/config/RedissonConfig.class",
                "**/config/SecurityConfig.class",
                "**/config/JacksonConfig.class",
                "**/config/Resilience4jConfig.class",
                // DTO classes
                "**/dto/**",
                "**/event/**",
                // Domain interfaces (no implementation)
                "**/domain/**/*Repository.class",
                // Feign client interface
                "**/feign/**"
            )
        }
    }))
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.50".toBigDecimal()
            }
        }
    }
}
val prometheusVersion = "0.16.0"
val logstashLogbackEncoderVersion = "8.0"
val opentelemetryLogbackMdcVersion = "2.16.0-alpha"


plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "no.nav.tsm"
version = "0.0.2"
java.sourceCompatibility = JavaVersion.VERSION_21


repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("net.logstash.logback:logstash-logback-encoder:${logstashLogbackEncoderVersion}")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-mdc-1.0:${opentelemetryLogbackMdcVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks {
    bootJar {
        archiveFileName = "app.jar"
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
    test {
        useJUnitPlatform()
        testLogging {
            events("skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}
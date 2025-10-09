import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val javaVersion = JvmTarget.JVM_21

//runtime deps
val logstashLogbackEncoderVersion = "8.1"
val sykmeldingInputVersion = "13"
val ibmMqVersion = "9.4.3.1"
val googleCloudStorageVersion = "2.58.1"
val syfoXmlCodegenVersion = "2.0.1"
val jaxbRuntimeVersion = "4.0.6"
val jaxbApiVersion = "2.4.0-b180830.0359"
val javaTimeAdapterVersion = "1.1.3"


// dev deps
val ktfmtVersion = "0.44"
val testContainersVersion = "1.21.3"

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20"
    id("org.springframework.boot") version "3.5.6"

    //other plugins
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.0.0"
    id("com.github.ben-manes.versions") version "0.53.0"
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

    // do we need oauth 2 resource server and client?
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("net.logstash.logback:logstash-logback-encoder:${logstashLogbackEncoderVersion}")
    implementation("no.nav.tsm.sykmelding", "input", sykmeldingInputVersion)
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("com.ibm.mq:com.ibm.mq.jakarta.client:${ibmMqVersion}")
    implementation("com.google.cloud:google-cloud-storage:${googleCloudStorageVersion}")
    implementation("no.nav.helse.xml:sm2013:${syfoXmlCodegenVersion}")
    implementation("no.nav.helse.xml:xmlfellesformat:${syfoXmlCodegenVersion}")
    implementation("no.nav.helse.xml:kith-hodemelding:${syfoXmlCodegenVersion}")
    implementation("no.nav.helse.xml:kith-apprec:${syfoXmlCodegenVersion}")
    implementation("javax.xml.bind:jaxb-api:${jaxbApiVersion}")
    implementation("org.glassfish.jaxb:jaxb-runtime:${jaxbRuntimeVersion}")
    implementation("com.migesok", "jaxb-java-time-adapters", javaTimeAdapterVersion)
    //TODO add open telemetry?

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:kafka:${testContainersVersion}")
    testImplementation("org.testcontainers:junit-jupiter:${testContainersVersion}")
}


kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        jvmTarget.set(javaVersion)
    }
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
    configure<SpotlessExtension> {
        kotlin { ktfmt(ktfmtVersion).kotlinlangStyle() }
        check {
            dependsOn("spotlessApply")
        }
    }
}
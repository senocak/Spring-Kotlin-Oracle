import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
}

group = "com.github.senocak"
version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

val jjwt = "0.11.5"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation(dependencyNotation = "com.oracle.database.jdbc:ojdbc11:23.6.0.24.10") {
        description = "Oracle JDBC Driver"
    }
    implementation(dependencyNotation = "com.oracle.database.jdbc:ucp11:23.6.0.24.10"){
        description = "Oracle Universal Connection Pool"
    }
    implementation(dependencyNotation = "com.oracle.database.spring:oracle-spring-boot-starter-json-collections:24.4.0") {
        description = "Oracle JSON Collections"
    }
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    implementation("org.flywaydb:flyway-core:11.1.0") // FlywayException: Unsupported Database: Oracle 23.6
    implementation("io.jsonwebtoken:jjwt-api:$jjwt")
    implementation("io.jsonwebtoken:jjwt-impl:$jjwt")
    implementation("io.jsonwebtoken:jjwt-jackson:$jjwt")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    maxHeapSize = "1G"
    val testType: String = "unit"
        .takeUnless { project.hasProperty("profile") }
        ?: "${project.property("profile")}"
    println(message = "Profile test type: $testType")
    when (testType) {
        "integration" -> include("**/*IT.*")
        else -> include("**/*Test.*")
    }
}

tasks.register<Test>("integrationTest") {
    description = "Runs the integration tests"
    group = "Verification"
    include("**/*IT.*")
}

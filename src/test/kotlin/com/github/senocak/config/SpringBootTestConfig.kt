package com.github.senocak.config

import com.github.senocak.config.initializer.CouchbaseInitializer
import org.junit.jupiter.api.ClassOrderer
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestClassOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Tag("integration")
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@ExtendWith(SpringExtension::class)
@Retention(AnnotationRetention.RUNTIME)
@ActiveProfiles(value = ["integration-test"])
@TestClassOrder(ClassOrderer.OrderAnnotation::class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(initializers = [
    CouchbaseInitializer::class,
])
annotation class SpringBootTestConfig
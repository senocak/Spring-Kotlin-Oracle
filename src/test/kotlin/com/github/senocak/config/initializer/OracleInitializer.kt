package com.github.senocak.config.initializer

import com.github.senocak.TestConstants.CONTAINER_WAIT_TIMEOUT
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.oracle.OracleContainer

@TestConfiguration
class OracleInitializer: ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        TestPropertyValues.of(
            "spring.datasource.url=${oracleContainer.jdbcUrl}",
            "spring.datasource.username=${oracleContainer.username}",
            "spring.datasource.password=${oracleContainer.password}",
            "spring.datasource.ddl=verify",
            "spring.jpa.hibernate.ddl-auto=none",
        ).applyTo(configurableApplicationContext.environment)
    }

    companion object {
        //private val couchbaseContainer = OracleContainer("gvenzl/oracle-free:23.4-slim-faststart")
        private val oracleContainer: OracleContainer = OracleContainer("gvenzl/oracle-free:slim-faststart")
            //.withExposedPorts(8091, 8092, 8093, 8094, 8095, 8096, 11210)
            .withDatabaseName("testDB")
            .withUsername("testUser")
            .withPassword("testPassword")
            .withDatabaseName("testDB")
            .withStartupTimeout(CONTAINER_WAIT_TIMEOUT)
            .withInitScripts("migration/V1__create_tables.sql", "migration/V2__insert_users_and_roles.sql")

        init {
            oracleContainer.start()
        }
    }
}
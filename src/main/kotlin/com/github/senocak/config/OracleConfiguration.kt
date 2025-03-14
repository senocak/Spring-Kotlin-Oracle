package com.github.senocak.config

import oracle.ucp.jdbc.PoolDataSource
import oracle.ucp.jdbc.PoolDataSourceFactory
import oracle.ucp.jdbc.PoolDataSourceImpl
import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.flyway.FlywayProperties
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class OracleConfiguration(
    private val flywayProperties: FlywayProperties,
    private val dataSourceProperties: DataSourceConfigs,
) {
    @Bean
    fun dataSource(): DataSource =
        PoolDataSourceFactory.getPoolDataSource()
            .also {dataSource: PoolDataSource ->
                dataSource.connectionFactoryClassName = dataSourceProperties.ucp.connectionFactoryClassName
                dataSource.url = dataSourceProperties.url
                dataSource.user = dataSourceProperties.username
                dataSource.password = dataSourceProperties.password
                // UCP-specific configurations
                dataSource.initialPoolSize = dataSourceProperties.ucp.initialPoolSize
                dataSource.minPoolSize = dataSourceProperties.ucp.minPoolSize
                dataSource.maxPoolSize = dataSourceProperties.ucp.maxPoolSize
                dataSource.timeoutCheckInterval = dataSourceProperties.ucp.timeoutCheckInterval
                dataSource.inactiveConnectionTimeout = dataSourceProperties.ucp.inactiveConnectionTimeout
                dataSource.sqlForValidateConnection = dataSourceProperties.ucp.sqlForValidateConnection
                dataSource.validateConnectionOnBorrow = dataSourceProperties.ucp.validateConnectionOnBorrow
                dataSource.secondsToTrustIdleConnection = dataSourceProperties.ucp.secondsToTrustIdleConnection
            }

    @Bean(initMethod = "migrate")
    fun flyway(dataSource: DataSource): Flyway =
        Flyway.configure()
            .dataSource(dataSource)
            .failOnMissingLocations(flywayProperties.isFailOnMissingLocations)
            .locations(flywayProperties.locations.joinToString(separator = ","))
            .defaultSchema(flywayProperties.defaultSchema)
            .table(flywayProperties.table)
            .sqlMigrationPrefix(flywayProperties.sqlMigrationPrefix)
            .sqlMigrationSeparator(flywayProperties.sqlMigrationSeparator)
            .load()
}

@Primary
@ConfigurationProperties(prefix = "spring.flyway")
class FlywayConfig: FlywayProperties()

@Primary
@ConfigurationProperties(prefix = "spring.datasource")
class DataSourceConfigs: DataSourceProperties() {
    lateinit var ddl: String
    lateinit var ucp: PoolDataSourceImpl
}
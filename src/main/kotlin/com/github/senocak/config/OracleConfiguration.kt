package com.github.senocak.config

import oracle.ucp.jdbc.PoolDataSource
import oracle.ucp.jdbc.PoolDataSourceFactory
import oracle.ucp.jdbc.PoolDataSourceImpl
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Configuration
class OracleConfiguration(private val dataSourceConfig: DataSourceConfig){
    @Bean
    fun dataSource(): DataSource =
        PoolDataSourceFactory.getPoolDataSource()
            .also {dataSource: PoolDataSource ->
                dataSource.connectionFactoryClassName = dataSourceConfig.ucp.connectionFactoryClassName
                dataSource.url = dataSourceConfig.url
                dataSource.user = dataSourceConfig.username
                dataSource.password = dataSourceConfig.password
                // UCP-specific configurations
                dataSource.initialPoolSize = dataSourceConfig.ucp.initialPoolSize
                dataSource.minPoolSize = dataSourceConfig.ucp.minPoolSize
                dataSource.maxPoolSize = dataSourceConfig.ucp.maxPoolSize
                dataSource.timeoutCheckInterval = dataSourceConfig.ucp.timeoutCheckInterval
                dataSource.inactiveConnectionTimeout = dataSourceConfig.ucp.inactiveConnectionTimeout
                dataSource.sqlForValidateConnection = dataSourceConfig.ucp.sqlForValidateConnection
                dataSource.validateConnectionOnBorrow = dataSourceConfig.ucp.validateConnectionOnBorrow
                dataSource.secondsToTrustIdleConnection = dataSourceConfig.ucp.secondsToTrustIdleConnection
            }
}

@Component
@ConfigurationProperties(prefix = "spring.datasource")
class DataSourceConfig {
    lateinit var url: String
    lateinit var username: String
    lateinit var password: String
    lateinit var ddl: String
    lateinit var ucp: PoolDataSourceImpl
}

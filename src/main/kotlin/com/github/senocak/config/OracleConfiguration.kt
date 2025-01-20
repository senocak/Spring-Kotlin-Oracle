package com.github.senocak.config

import oracle.ucp.jdbc.PoolDataSource
import oracle.ucp.jdbc.PoolDataSourceFactory
import oracle.ucp.jdbc.PoolDataSourceImpl
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
@ConfigurationProperties(prefix = "spring.datasource")
class OracleConfiguration{
    lateinit var url: String
    lateinit var username: String
    lateinit var password: String
    lateinit var ddl: String
    lateinit var ucp: PoolDataSourceImpl

    @Bean
    fun dataSource(): DataSource =
        PoolDataSourceFactory.getPoolDataSource()
            .also {dataSource: PoolDataSource ->
                dataSource.connectionFactoryClassName = ucp.connectionFactoryClassName
                dataSource.url = url
                dataSource.user = username
                dataSource.password = password
                // UCP-specific configurations
                dataSource.initialPoolSize = ucp.initialPoolSize
                dataSource.minPoolSize = ucp.minPoolSize
                dataSource.maxPoolSize = ucp.maxPoolSize
                dataSource.timeoutCheckInterval = ucp.timeoutCheckInterval
                dataSource.inactiveConnectionTimeout = ucp.inactiveConnectionTimeout
                dataSource.sqlForValidateConnection = ucp.sqlForValidateConnection
                dataSource.validateConnectionOnBorrow = ucp.validateConnectionOnBorrow
                dataSource.secondsToTrustIdleConnection = ucp.secondsToTrustIdleConnection
            }
}
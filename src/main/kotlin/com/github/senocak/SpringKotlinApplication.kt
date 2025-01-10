package com.github.senocak

import com.github.senocak.util.logger
import oracle.ucp.jdbc.PoolDataSource
import org.slf4j.Logger
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import javax.sql.DataSource

@SpringBootApplication
@EnableScheduling
class SpringKotlinApplication(
    private val dataSource: DataSource
) {
    private val log: Logger by logger()

    @Scheduled(cron = "0 0/1 * * * ?")
    fun logPoolStatistics() {
        val poolDataSource: PoolDataSource = dataSource as PoolDataSource
        log.info("""
        Initial Pool Size: ${poolDataSource.initialPoolSize}
        Min Pool Size: ${poolDataSource.minPoolSize}
        Max Pool Size: ${poolDataSource.maxPoolSize}
        Timeout Check Interval: ${poolDataSource.timeoutCheckInterval}
        Inactive Connection Timeout: ${poolDataSource.inactiveConnectionTimeout}
        SQL For Validate Connection: ${poolDataSource.sqlForValidateConnection}
        Available Connections: ${poolDataSource.availableConnectionsCount}
        Borrowed Connections: ${poolDataSource.borrowedConnectionsCount}
        Abandoned Connections: ${poolDataSource.abandonedConnectionTimeout}
        """)
    }
}

fun main(args: Array<String>) {
    runApplication<SpringKotlinApplication>(*args)
}

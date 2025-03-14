package com.github.senocak.service

import com.github.senocak.util.Log
import com.github.senocak.util.logger
import com.sun.management.OperatingSystemMXBean
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import oracle.ucp.admin.UniversalConnectionPoolManager
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl
import oracle.ucp.jdbc.PoolDataSource
import org.slf4j.Logger
import org.slf4j.MDC
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.lang.management.ManagementFactory
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import javax.sql.DataSource

@Async
@Component
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT60S")
class ScheduledTasks(private val dataSource: DataSource){
    private val log: Logger by logger()
    private val dateFormat = SimpleDateFormat("YYYY-MM-DD HH:mm:ss")
    private val operatingSystemMXBean: OperatingSystemMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)

    private val byte = 1L
    private val kb: Long = byte * 1000
    private val mb: Long = kb * 1000
    private val gb: Long = mb * 1000
    private val tb: Long = gb * 1000

    @Scheduled(cron = "0/20 * * * * ?")
    @SchedulerLock(name = "logPoolStatistics", lockAtMostFor = "PT30S", lockAtLeastFor = "5s")
    @Log
    fun logPoolStatistics() {
        val mgr: UniversalConnectionPoolManager = UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager()
        log.info("${mgr.getConnectionPool(mgr.connectionPoolNames.first()).statistics}")
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

    /**
     * Configurations related to Scheduler and ShedLock to ensure that only one node triggers the scheduled task in a multi-node environment.
     */
    @SchedulerLock(name = "logPerformance", lockAtMostFor = "PT30S", lockAtLeastFor = "5s")
    @Scheduled(cron = "0 * * ? * *")
    @Log
    fun logPerformance() =
        Runtime.getRuntime()
            .apply { MDC.put("userId", "scheduler") }
            .run {
                Performance(
                    timestamp = dateFormat.format(Date()),
                    committedVirtualMemorySize = operatingSystemMXBean.committedVirtualMemorySize,
                    totalSwapSpaceSize = operatingSystemMXBean.totalSwapSpaceSize,
                    freeSwapSpaceSize = operatingSystemMXBean.freeSwapSpaceSize,
                    totalMemorySize = operatingSystemMXBean.totalMemorySize,
                    freeMemorySize = operatingSystemMXBean.freeMemorySize,
                    cpuLoad = operatingSystemMXBean.cpuLoad,
                    processCpuLoad = operatingSystemMXBean.processCpuLoad,
                    availableProcessors = this.availableProcessors(),
                    totalMemory = toHumanReadableSIPrefixes(size = this.totalMemory()),
                    maxMemory = toHumanReadableSIPrefixes(size = this.maxMemory()),
                    freeMemory = toHumanReadableSIPrefixes(size = this.freeMemory())
                )
            }
            .run { log.info("$this") }
            .run { MDC.remove("userId") }

    private fun toHumanReadableSIPrefixes(size: Long): String =
        when {
            size >= tb -> formatSize(size = size, divider = tb, unitName = "TB")
            size >= gb -> formatSize(size = size, divider = gb, unitName = "GB")
            size >= mb -> formatSize(size = size, divider = mb, unitName = "MB")
            size >= kb -> formatSize(size = size, divider = kb, unitName = "KB")
            else -> formatSize(size = size, divider = byte, unitName = "Bytes")
        }

    private fun formatSize(size: Long, divider: Long, unitName: String): String =
        DecimalFormat("#.##").format(size.toDouble() / divider) + " " + unitName
}

internal data class Performance(
    val timestamp: String,
    var committedVirtualMemorySize: Long = 0,
    var totalSwapSpaceSize: Long = 0,
    var freeSwapSpaceSize: Long = 0,
    var totalMemorySize: Long = 0,
    var freeMemorySize: Long = 0,
    var cpuLoad: Double = 0.0,
    var processCpuLoad: Double = 0.0,
    var availableProcessors: Int = 0,
    var totalMemory: String = "",
    var maxMemory: String = "",
    var freeMemory: String = "",
)
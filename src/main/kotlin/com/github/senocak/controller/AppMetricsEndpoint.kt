package com.github.senocak.controller

import com.github.senocak.config.LockConfig
import com.github.senocak.service.RedisService
import com.github.senocak.util.logger
import net.javacrumbs.shedlock.core.LockAssert
import org.slf4j.Logger
import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.info.BuildProperties
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Connection

/**
 * Custom metrics endpoint that provides detailed application health and metrics information.
 * Available at /actuator/appHealth
 *
 * Performance metrics include both global and per-endpoint statistics:
 * 
 * Metrics are organized hierarchically:
 * - Global application metrics
 * - HTTP method level (GET, POST, etc.)
 *   - Path-specific metrics
 *     - Request counts
 *     - Response times
 *     - Error rates
 * Global metrics:
 *  - Overall application performance
 *  - Total request counts and error rates
 *  - System-wide response time statistics
 *
 * Per-endpoint metrics:
 *  - Individual endpoint performance
 *  - Endpoint-specific request counts and error rates
 *  - Detailed response time statistics per endpoint
 * - Average response time
 * - Maximum and minimum response times
 * - 95th percentile response time (calculated from last 1000 requests)
 * - Request counts and error rates
 *
 * Provides:
 * - Application metrics (requests, errors, uptime)
 * - System metrics (memory, processors, JVM)
 * - Connection status (Redis, Database)
 * - Security status
 * - Build information
 */
@Component
@Endpoint(id = "appHealth")
class AppMetricsEndpoint(
    private val redisService: RedisService,
    private val jdbcTemplate: JdbcTemplate,
    private val buildProperties: BuildProperties,
    private val health: HealthEndpoint,
) {
    private val log: Logger by logger()
    private var startTime: Long = System.currentTimeMillis()
    private var requestCount: Long = 0
    private var errorCount: Long = 0
    private var totalResponseTime: Long = 0
    private var maxResponseTime: Long = 0
    private var minResponseTime: Long = Long.MAX_VALUE
    private val responseTimes: MutableList<Long> = mutableListOf()
    private val maxSampleSize: Int = 1000 // Keep last 1000 requests for percentile calculation
    private val endpointMetrics: MutableMap<String, MutableMap<String, EndpointMetrics>> = mutableMapOf()

    private fun getOrCreateMetrics(method: String, path: String): EndpointMetrics {
        return synchronized(endpointMetrics) {
            endpointMetrics
                .getOrPut(method) { mutableMapOf() }
                .getOrPut(path) { EndpointMetrics() }
        }
    }

    data class TimeBasedMetrics(
        var requestCount: Long = 0,
        var errorCount: Long = 0,
        var totalResponseTime: Long = 0,
        var maxResponseTime: Long = 0,
        var minResponseTime: Long = Long.MAX_VALUE,
        val responseTimes: MutableList<Long> = mutableListOf()
    ) {
        companion object {
            private const val MAX_SAMPLES = 1000
        }
        fun addResponseTime(responseTime: Long) {
            requestCount++
            totalResponseTime += responseTime
            maxResponseTime = maxOf(maxResponseTime, responseTime)
            minResponseTime = minOf(minResponseTime, responseTime)
            if (responseTimes.size >= MAX_SAMPLES) {
                responseTimes.removeAt(0)
            }
            responseTimes.add(responseTime)
        }

        fun calculate95thPercentile(): Long {
            return if (responseTimes.isEmpty()) 0
            else {
                val sortedTimes = responseTimes.sorted()
                val index = ((responseTimes.size - 1) * 0.95).toInt()
                sortedTimes[index]
            }
        }
    }

    data class EndpointMetrics(
        var requestCount: Long = 0,
        var errorCount: Long = 0,
        var totalResponseTime: Long = 0,
        var maxResponseTime: Long = 0,
        var minResponseTime: Long = Long.MAX_VALUE,
        val responseTimes: MutableList<Long> = mutableListOf(),
        val dailyMetrics: MutableMap<String, TimeBasedMetrics> = mutableMapOf(),
        val hourlyMetrics: MutableMap<String, TimeBasedMetrics> = mutableMapOf()
    ) {
        companion object {
            private const val MAX_SAMPLES = 1000
            fun getDailyKey(): String = java.time.LocalDate.now().toString()
            fun getHourlyKey(): String = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH"))
        }

        fun getOrCreateTimeBasedMetrics(key: String, metricsMap: MutableMap<String, TimeBasedMetrics>): TimeBasedMetrics {
            return metricsMap.getOrPut(key) { TimeBasedMetrics() }
        }

        fun addResponseTime(responseTime: Long) {
            synchronized(this) {
                // Update overall metrics
                requestCount++
                totalResponseTime += responseTime
                maxResponseTime = maxOf(maxResponseTime, responseTime)
                minResponseTime = minOf(minResponseTime, responseTime)
                if (responseTimes.size >= MAX_SAMPLES) {
                    responseTimes.removeAt(0)
                }
                responseTimes.add(responseTime)

                // Update daily metrics
                val dailyKey = getDailyKey()
                getOrCreateTimeBasedMetrics(dailyKey, dailyMetrics).addResponseTime(responseTime)

                // Update hourly metrics
                val hourlyKey = getHourlyKey()
                getOrCreateTimeBasedMetrics(hourlyKey, hourlyMetrics).addResponseTime(responseTime)
            }
        }

        fun calculate95thPercentile(): Long {
            return if (responseTimes.isEmpty()) 0
            else {
                val sortedTimes: List<Long> = responseTimes.sorted()
                val index: Int = ((responseTimes.size - 1) * 0.95).toInt()
                sortedTimes[index]
            }
        }
    }

    /**
     * Adds a response time measurement to the metrics.
     * Updates total response time and maintains min/max values.
     * @param responseTime The response time in milliseconds
     * @param method The HTTP method
     * @param path The request path
     */
    fun addResponseTime(responseTime: Long, method: String, path: String, controller: String) {
        // Update global metrics
        totalResponseTime += responseTime
        maxResponseTime = maxOf(a = maxResponseTime, b = responseTime)
        minResponseTime = minOf(a = minResponseTime, b = responseTime)
        synchronized(responseTimes) {
            if (responseTimes.size >= maxSampleSize) {
                responseTimes.removeAt(index = 0)
            }
            responseTimes.add(element = responseTime)
        }

        // Update endpoint-specific metrics
        getOrCreateMetrics(method = method, path = path)
            .addResponseTime(responseTime = responseTime)
    }

    fun incrementErrorCount(method: String, path: String) {
        errorCount++
        val metrics = getOrCreateMetrics(method = method, path = path)
        synchronized(metrics) {
            metrics.errorCount++

            // Update daily metrics
            val dailyKey = EndpointMetrics.getDailyKey()
            metrics.getOrCreateTimeBasedMetrics(dailyKey, metrics.dailyMetrics).errorCount++

            // Update hourly metrics
            val hourlyKey = EndpointMetrics.getHourlyKey()
            metrics.getOrCreateTimeBasedMetrics(hourlyKey, metrics.hourlyMetrics).errorCount++
        }
    }

    fun incrementRequestCount(): Long = requestCount++
    fun incrementErrorCount(): Long = errorCount++

    private fun calculate95thPercentile(): Long =
        synchronized(responseTimes) {
            if (responseTimes.isEmpty()) 0
            else {
                val sortedTimes: List<Long> = responseTimes.sorted()
                val index: Int = ((responseTimes.size - 1) * 0.95).toInt()
                sortedTimes[index]
            }
        }

    @ReadOperation
    fun metrics(): Map<String, Any> =
        mapOf(
            "application" to mapOf(
                "status" to "UP",
                "startTime" to startTime,
                "uptime" to (System.currentTimeMillis() - startTime),
                "requests" to mapOf(
                    "total" to requestCount,
                    "errors" to errorCount,
                    "successRate" to if (requestCount > 0) ((requestCount - errorCount) * 100.0 / requestCount) else 100.0,
                    "performance" to mapOf(
                        "avgResponseTime" to if (requestCount > 0) totalResponseTime.toDouble() / requestCount else 0.0,
                        "maxResponseTime" to maxResponseTime,
                        "minResponseTime" to if (minResponseTime == Long.MAX_VALUE) 0 else minResponseTime,
                        "p95ResponseTime" to calculate95thPercentile()
                    )
                ),
                "endpoints" to synchronized(endpointMetrics) {
                    endpointMetrics.mapValues { (method: String, pathMetrics: MutableMap<String, EndpointMetrics>) ->
                        pathMetrics.mapValues { (_, metrics: EndpointMetrics) ->
                            mapOf(
                                "overall" to mapOf(
                                    "requestCount" to metrics.requestCount,
                                    "errorCount" to metrics.errorCount,
                                    "avgResponseTime" to if (metrics.requestCount > 0) 
                                        metrics.totalResponseTime.toDouble() / metrics.requestCount else 0.0,
                                    "maxResponseTime" to metrics.maxResponseTime,
                                    "minResponseTime" to if (metrics.minResponseTime == Long.MAX_VALUE) 0 
                                        else metrics.minResponseTime,
                                    "p95ResponseTime" to metrics.calculate95thPercentile()
                                ),
                                "daily" to metrics.dailyMetrics.mapValues { (_, dailyMetric) ->
                                    mapOf(
                                        "requestCount" to dailyMetric.requestCount,
                                        "errorCount" to dailyMetric.errorCount,
                                        "avgResponseTime" to if (dailyMetric.requestCount > 0)
                                            dailyMetric.totalResponseTime.toDouble() / dailyMetric.requestCount else 0.0,
                                        "maxResponseTime" to dailyMetric.maxResponseTime,
                                        "minResponseTime" to if (dailyMetric.minResponseTime == Long.MAX_VALUE) 0
                                            else dailyMetric.minResponseTime,
                                        "p95ResponseTime" to dailyMetric.calculate95thPercentile()
                                    )
                                },
                                "hourly" to metrics.hourlyMetrics.mapValues { (_, hourlyMetric) ->
                                    mapOf(
                                        "requestCount" to hourlyMetric.requestCount,
                                        "errorCount" to hourlyMetric.errorCount,
                                        "avgResponseTime" to if (hourlyMetric.requestCount > 0)
                                            hourlyMetric.totalResponseTime.toDouble() / hourlyMetric.requestCount else 0.0,
                                        "maxResponseTime" to hourlyMetric.maxResponseTime,
                                        "minResponseTime" to if (hourlyMetric.minResponseTime == Long.MAX_VALUE) 0
                                            else hourlyMetric.minResponseTime,
                                        "p95ResponseTime" to hourlyMetric.calculate95thPercentile()
                                    )
                                }
                            )
                        }
                    }
                }
            ),
            "system" to mapOf(
                "memory" to mapOf(
                    "total" to Runtime.getRuntime().totalMemory(),
                    "free" to Runtime.getRuntime().freeMemory(),
                    "max" to Runtime.getRuntime().maxMemory()
                ),
                "processors" to Runtime.getRuntime().availableProcessors(),
                "jvm" to mapOf(
                    "version" to System.getProperty("java.version"),
                    "vendor" to System.getProperty("java.vendor")
                )
            ),
            "connections" to mapOf(
                "redis" to mapOf(
                    "alive" to redisService.ping(),
                    "jedisPool" to redisService.jedisPool(),
                    "schedulers" to redisService.getAllValuesForPattern(pattern = "job-lock:${LockConfig.ENV}")
                ),
                "database" to mapOf(
                    "alive" to runCatching { 
                        jdbcTemplate.dataSource?.connection?.use { conn: Connection ->
                            !conn.isClosed
                        } ?: false
                    }.getOrDefault(defaultValue = false)
                )
            ),
            "security" to mapOf(
                "lock" to runCatching { LockAssert.assertLocked(); "locked" }.getOrElse { "not locked" }
            ),
            "build" to buildProperties,
            "health" to health.health()
        ).also { log.info("Metrics: $it") }
}

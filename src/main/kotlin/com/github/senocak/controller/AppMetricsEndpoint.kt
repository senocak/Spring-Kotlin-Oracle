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
import redis.clients.jedis.JedisPool

/**
 * Custom metrics endpoint that provides detailed application health and metrics information.
 * Available at /actuator/appHealth
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
    private val jedisPool: JedisPool,
    private val jdbcTemplate: JdbcTemplate,
    private val buildProperties: BuildProperties,
    private val health: HealthEndpoint,
) {
    private val log: Logger by logger()



    private var startTime: Long = System.currentTimeMillis()
    private var requestCount: Long = 0
    private var errorCount: Long = 0

    fun incrementRequestCount() {
        requestCount++
    }

    fun incrementErrorCount() {
        errorCount++
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
                    "successRate" to if (requestCount > 0) ((requestCount - errorCount) * 100.0 / requestCount) else 100.0
                )
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
                    "alive" to (jedisPool.resource.ping() != null),
                    "schedulers" to redisService.getAllValuesForPattern(pattern = "job-lock:${LockConfig.ENV}")
                ),
                "database" to mapOf(
                    "alive" to runCatching { 
                        jdbcTemplate.dataSource?.connection?.use { conn ->
                            !conn.isClosed
                        } ?: false
                    }.getOrDefault(false)
                )
            ),
            "security" to mapOf(
                "lock" to runCatching { LockAssert.assertLocked(); "locked" }.getOrElse { "not locked" }
            ),
            "build" to buildProperties,
            "health" to health.health()
        ).also { log.info("Metrics: $it") }
}

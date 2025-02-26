package com.github.senocak.controller

import com.github.senocak.service.RedisService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.mock
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.info.BuildProperties
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppMetricsEndpointTest {
    private lateinit var appMetricsEndpoint: AppMetricsEndpoint
    private lateinit var redisService: RedisService
    private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var buildProperties: BuildProperties
    private lateinit var healthEndpoint: HealthEndpoint

    @BeforeEach
    fun setup() {
        redisService = mock(RedisService::class.java)
        jdbcTemplate = mock(JdbcTemplate::class.java)
        buildProperties = mock(BuildProperties::class.java)
        healthEndpoint = mock(HealthEndpoint::class.java)
        appMetricsEndpoint = AppMetricsEndpoint(redisService, jdbcTemplate, buildProperties, healthEndpoint)
    }

    @Test
    fun `test time-based metrics collection`() {
        // Given
        val method = "GET"
        val path = "/api/test"
        val responseTime = 100L

        // When
        appMetricsEndpoint.addResponseTime(responseTime, method, path, "TestController")
        appMetricsEndpoint.incrementErrorCount(method, path)

        // Then
        val metrics = appMetricsEndpoint.metrics()
        val endpoints = metrics["application"]!! as Map<*, *>
        val requests = endpoints["requests"]!! as Map<*, *>
        val endpointMetrics = endpoints["endpoints"]!! as Map<*, *>
        val methodMetrics = endpointMetrics[method]!! as Map<*, *>
        val pathMetrics = methodMetrics[path]!! as Map<*, *>

        // Verify overall metrics
        val overall = pathMetrics["overall"]!! as Map<*, *>
        assertEquals(1L, overall["requestCount"])
        assertEquals(1L, overall["errorCount"])
        assertEquals(100.0, overall["avgResponseTime"])
        assertEquals(100L, overall["maxResponseTime"])
        assertEquals(100L, overall["minResponseTime"])

        // Verify daily metrics
        val daily = pathMetrics["daily"]!! as Map<*, *>
        val todayKey = LocalDate.now().toString()
        val todayMetrics = daily[todayKey]!! as Map<*, *>
        assertEquals(1L, todayMetrics["requestCount"])
        assertEquals(1L, todayMetrics["errorCount"])
        assertEquals(100.0, todayMetrics["avgResponseTime"])

        // Verify hourly metrics
        val hourly = pathMetrics["hourly"]!! as Map<*, *>
        val hourKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH"))
        val hourMetrics = hourly[hourKey]!! as Map<*, *>
        assertEquals(1L, hourMetrics["requestCount"])
        assertEquals(1L, hourMetrics["errorCount"])
        assertEquals(100.0, hourMetrics["avgResponseTime"])
    }

    @Test
    fun `test multiple requests in different time periods`() {
        // Given
        val method = "POST"
        val path = "/api/test"
        
        // When - simulate multiple requests
        repeat(3) {
            appMetricsEndpoint.addResponseTime(100L, method, path, "TestController")
        }
        appMetricsEndpoint.incrementErrorCount(method, path)

        // Then
        val metrics = appMetricsEndpoint.metrics()
        val endpoints = metrics["application"]!! as Map<*, *>
        val endpointMetrics = endpoints["endpoints"]!! as Map<*, *>
        val methodMetrics = endpointMetrics[method]!! as Map<*, *>
        val pathMetrics = methodMetrics[path]!! as Map<*, *>

        // Verify overall metrics
        val overall = pathMetrics["overall"]!! as Map<*, *>
        assertEquals(3L, overall["requestCount"])
        assertEquals(1L, overall["errorCount"])
        assertEquals(100.0, overall["avgResponseTime"])

        // Verify daily metrics
        val daily = pathMetrics["daily"]!! as Map<*, *>
        val todayKey = LocalDate.now().toString()
        val todayMetrics = daily[todayKey]!! as Map<*, *>
        assertEquals(3L, todayMetrics["requestCount"])
        assertEquals(1L, todayMetrics["errorCount"])
        assertEquals(100.0, todayMetrics["avgResponseTime"])
    }

    @Test
    fun `test response time statistics`() {
        // Given
        val method = "GET"
        val path = "/api/test"
        
        // When - add various response times
        val responseTimes = listOf(50L, 100L, 150L, 200L, 250L)
        responseTimes.forEach {
            appMetricsEndpoint.addResponseTime(it, method, path, "TestController")
        }

        // Then
        val metrics = appMetricsEndpoint.metrics()
        val endpoints = metrics["application"]!! as Map<*, *>
        val endpointMetrics = endpoints["endpoints"]!! as Map<*, *>
        val methodMetrics = endpointMetrics[method]!! as Map<*, *>
        val pathMetrics = methodMetrics[path]!! as Map<*, *>

        // Verify overall metrics
        val overall = pathMetrics["overall"]!! as Map<*, *>
        assertEquals(5L, overall["requestCount"])
        assertEquals(150.0, overall["avgResponseTime"])
        assertEquals(250L, overall["maxResponseTime"])
        assertEquals(50L, overall["minResponseTime"])

        // Verify daily metrics
        val daily = pathMetrics["daily"]!! as Map<*, *>
        val todayKey = LocalDate.now().toString()
        val todayMetrics = daily[todayKey]!! as Map<*, *>
        assertEquals(5L, todayMetrics["requestCount"])
        assertEquals(150.0, todayMetrics["avgResponseTime"])
        assertEquals(250L, todayMetrics["maxResponseTime"])
        assertEquals(50L, todayMetrics["minResponseTime"])
    }
}
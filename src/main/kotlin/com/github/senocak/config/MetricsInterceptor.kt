package com.github.senocak.config

import com.github.senocak.controller.AppMetricsEndpoint
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.method.HandlerMethod

/**
 * Interceptor for tracking request metrics including response times.
 * Captures:
 * - Request counts
 * - Error counts
 * - Response times (min, max, average)
 *
 * Response time is measured from the start of request processing
 * until after the completion of the request.
 */
@Component
class MetricsInterceptor(
    @Lazy private val metricsEndpoint: AppMetricsEndpoint
) : HandlerInterceptor {
    companion object {
        private const val START_TIME_ATTRIBUTE = "requestStartTime"
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis())
        metricsEndpoint.incrementRequestCount()
        return true
    }

    private fun extractControllerName(handler: Any): String {
        return when (handler) {
            is HandlerMethod -> handler.beanType.simpleName
            else -> "unknown"
        }
    }

    override fun afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception?) {
        val startTime: Long = request.getAttribute(START_TIME_ATTRIBUTE) as Long
        val responseTime: Long = System.currentTimeMillis() - startTime
        val method = request.method
        val path = request.requestURI
        val controller = extractControllerName(handler)

        metricsEndpoint.addResponseTime(
            responseTime = responseTime,
            method = method,
            path = path,
            controller = controller
        )

        if (ex != null || response.status >= 400) {
            metricsEndpoint.incrementErrorCount(
                method = method,
                path = path
            )
        }
    }
}

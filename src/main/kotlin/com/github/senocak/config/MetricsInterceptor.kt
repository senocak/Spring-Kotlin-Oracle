package com.github.senocak.config

import com.github.senocak.controller.AppMetricsEndpoint
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class MetricsInterceptor(
    private val metricsEndpoint: AppMetricsEndpoint
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        metricsEndpoint.incrementRequestCount()
        return true
    }

    override fun afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception?) {
        if (ex != null || response.status >= 400) {
            metricsEndpoint.incrementErrorCount()
        }
    }
}
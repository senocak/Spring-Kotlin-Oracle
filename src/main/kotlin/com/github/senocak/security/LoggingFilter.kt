package com.github.senocak.security

import com.github.senocak.util.logger
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.io.IOException

@Component
class LoggingFilter : OncePerRequestFilter(), Ordered {
    private val log: Logger by logger()
    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val wrappedRequest = ContentCachingRequestWrapper(request)
        val wrappedResponse = ContentCachingResponseWrapper(response)
        if (wrappedRequest.contentAsByteArray.isNotEmpty()) {
            val requestBody: String = String(wrappedRequest.contentAsByteArray, charset(request.characterEncoding))
            log.info("Incoming request: {} {} - body: {}", request.method, request.requestURI, requestBody)
        }
        filterChain.doFilter(wrappedRequest, wrappedResponse)
        if (wrappedResponse.contentAsByteArray.isNotEmpty()) {
            val responseBody: String = String(wrappedResponse.contentAsByteArray, charset(response.characterEncoding))
            log.info("Outgoing response: status={} - body: {}", response.status, responseBody)
        }
        wrappedResponse.copyBodyToResponse() // important!
    }
}
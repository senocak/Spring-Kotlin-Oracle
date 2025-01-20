package com.github.senocak.service

import com.fasterxml.uuid.Generators
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.security.web.csrf.DefaultCsrfToken
import org.springframework.stereotype.Repository

@Repository
// for db: https://www.java4coding.com/contents/springsecurity/custom-csrftokenrepository-in-spring-security
class MyCookieCsrfTokenRepository(
    private val redisTemplate: RedisTemplate<String, Any>
) : CsrfTokenRepository {
    private val valueOps: ValueOperations<String, Any> = redisTemplate.opsForValue()

    /**
     * Generates a new CSRF token, saves it in the Redis store and the HTTP session, and returns it.
     *
     * @param request the HTTP request
     * @return the generated CSRF token
     */
    override fun generateToken(request: HttpServletRequest?): CsrfToken {
        val token = DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", Generators.timeBasedEpochGenerator().generate().toString())
        saveToken(token = token, request = request, response = null)
        return token
    }

    /**
     * Saves the given CSRF token in the Redis store and the HTTP session.
     *
     * @param token the CSRF token to save
     * @param request the HTTP request
     * @param response the HTTP response (not used in this implementation)
     */
    override fun saveToken(token: CsrfToken?, request: HttpServletRequest?, response: HttpServletResponse?) {
        if (token == null) {
            request?.session?.removeAttribute("csrfToken")
        } else {
            valueOps.set(token.token, token)
            request?.session?.setAttribute("csrfToken", token)
        }
    }

    /**
     * Loads the CSRF token from the HTTP session or Redis store.
     *
     * @param request the HTTP request
     * @return the loaded CSRF token, or null if not found
     */
    override fun loadToken(request: HttpServletRequest?): CsrfToken? {
        val token: CsrfToken? = request?.session?.getAttribute("csrfToken") as? CsrfToken
        return token ?: valueOps.get("${request?.getHeader("X-XSRF-TOKEN")}") as? CsrfToken
    }
}
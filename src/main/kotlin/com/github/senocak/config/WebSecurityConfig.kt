package com.github.senocak.config

import com.github.senocak.controller.BaseController
import com.github.senocak.security.CustomAuthenticationManager
import com.github.senocak.security.JwtAuthenticationEntryPoint
import com.github.senocak.security.JwtAuthenticationFilter
import com.github.senocak.service.MyCookieCsrfTokenRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository
import org.springframework.security.web.util.matcher.RequestMatcher

@Configuration
@EnableWebSecurity
class WebSecurityConfig(
    private val unauthorizedHandler: JwtAuthenticationEntryPoint,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val customAuthenticationManager: CustomAuthenticationManager,
    private val myCookieCsrfTokenRepository: MyCookieCsrfTokenRepository
) {
    /**
     * Override this method to configure the HttpSecurity.
     * @param http -- It allows configuring web based security for specific http requests
     * @throws Exception -- throws Exception
     */
    @Profile("!integration-test")
    @Bean
    fun securityFilterChainDSL(http: HttpSecurity): SecurityFilterChain =
        http {
            csrf {
                //csrfTokenRepository = crossSiteRequestForgery() //CookieCsrfTokenRepository.withHttpOnlyFalse()
                csrfTokenRepository = myCookieCsrfTokenRepository
                requireCsrfProtectionMatcher = RequestMatcher { it.method == "POST" }
            }
            formLogin { disable() }
            httpBasic { disable() }
            exceptionHandling { authenticationEntryPoint = unauthorizedHandler }
            authorizeHttpRequests {
                authorize(pattern = "${BaseController.V1_AUTH_URL}/**", access = permitAll)
                authorize(pattern = "${BaseController.V1}/public/**", access = permitAll)
                authorize(pattern = "${BaseController.V1}/swagger/**", access = permitAll)
                authorize(pattern = "/swagger**/**", access = permitAll)
                authorize(pattern = "/error**/**", access = permitAll)
                authorize(matches = anyRequest, access = authenticated)
            }
            authenticationManager = customAuthenticationManager
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            headers { frameOptions { disable() } }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(filter = jwtAuthenticationFilter)
        }
        .run { http.build() }

    @Bean
    fun crossSiteRequestForgery(): CsrfTokenRepository {
        val repository = HttpSessionCsrfTokenRepository()
        repository.setHeaderName("X-XSRF-TOKEN")
        return repository
    }
}

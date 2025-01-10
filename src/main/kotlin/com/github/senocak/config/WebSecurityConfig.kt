package com.github.senocak.config

import com.github.senocak.controller.BaseController
import com.github.senocak.domain.Role
import com.github.senocak.domain.User
import com.github.senocak.security.JwtAuthenticationEntryPoint
import com.github.senocak.security.JwtAuthenticationFilter
import com.github.senocak.service.UserService
import com.github.senocak.util.RoleName
import com.github.senocak.util.logger
import org.slf4j.Logger
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class WebSecurityConfig(
    private val unauthorizedHandler: JwtAuthenticationEntryPoint,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val customAuthenticationManager: AuthenticationManager
) {

    /**
     * Override this method to configure the HttpSecurity.
     * @param http -- It allows configuring web based security for specific http requests
     * @throws Exception -- throws Exception
     */
    @Profile("!integration-test")
    @Bean
    fun securityFilterChainDSL(http: HttpSecurity, customAuthenticationProvider: AuthenticationProvider): SecurityFilterChain =
        http {
            csrf { disable() }
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
    fun customAuthenticationProvider(userService: UserService, passwordEncoder: PasswordEncoder): AuthenticationProvider {
        return object : AuthenticationProvider {
            override fun authenticate(authentication: Authentication): Authentication {
                val user: User = userService.findByEmail(authentication.name)
                if (authentication.credentials != null) {
                    val matches = passwordEncoder.matches(authentication.credentials.toString(), user.password)
                    if (!matches) {
                        throw BadCredentialsException("Invalid username or password")
                    }
                }
                val authorities = mutableListOf(SimpleGrantedAuthority(RoleName.ROLE_USER.role))
                if (user.roles.any { r: Role -> r.name!! == RoleName.ROLE_ADMIN })
                    authorities.add(element = SimpleGrantedAuthority(RoleName.ROLE_ADMIN.role))
                return UsernamePasswordAuthenticationToken(user, authentication.credentials, authorities)
            }

            override fun supports(authentication: Class<*>): Boolean =
                UsernamePasswordAuthenticationToken::class.java.isAssignableFrom(authentication)
        }
    }
}

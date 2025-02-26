package com.github.senocak.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.senocak.security.AuthorizationInterceptor
import com.github.senocak.util.AppConstants.SECURITY_SCHEME_NAME
import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointProperties
import org.springframework.boot.actuate.endpoint.Show
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.client.RestTemplate
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Duration

@EnableAsync
@Configuration
class AppConfig(
    private val authorizationInterceptor: AuthorizationInterceptor,
    private val metricsInterceptor: MetricsInterceptor,
    private val buildProperties: BuildProperties
): WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addRedirectViewController("/", "/index.html")
        registry.addRedirectViewController("/swagger", "/swagger-ui/index.html")
    }

    /**
     * Marking the files as resource
     * @param registry -- Stores registrations of resource handlers for serving static resources
     */
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry
            .addResourceHandler("//**")
            .addResourceLocations("classpath:/static/")
    }

    /**
     * Add Spring MVC lifecycle interceptors for pre- and post-processing of controller method invocations
     * and resource handler requests.
     * @param registry -- List of mapped interceptors.
     */
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(authorizationInterceptor)
            .addPathPatterns("/api/v1/**")

        registry
            .addInterceptor(metricsInterceptor)
            .addPathPatterns("/**")
    }

    @Bean
    fun modelResolver(objectMapper: ObjectMapper): ModelResolver = ModelResolver(objectMapper)

    @Bean
    fun customOpenAPI(
        @Value("\${server.port}") port: String
    ): OpenAPI {
        val securitySchemesItem: SecurityScheme = SecurityScheme()
            .name(SECURITY_SCHEME_NAME)
            .description("JWT auth description")
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .`in`(SecurityScheme.In.HEADER)
            .bearerFormat("JWT")
        val license: Info = Info().title(buildProperties.name).version(buildProperties.version)
            .description(buildProperties.name)
            .termsOfService("https://github.com/senocak")
            .license(License().name("Apache 2.0").url("https://springdoc.org"))
        val server1: Server = Server().url("http://localhost:$port/").description("Local Server")
        return OpenAPI()
            .components(Components().addSecuritySchemes(SECURITY_SCHEME_NAME, securitySchemesItem))
            .info(license)
            .servers(listOf(element = server1))
    }

    @Bean
    fun accountApi(): GroupedOpenApi =
        GroupedOpenApi.builder().displayName("User operations").group("user").pathsToMatch("/api/v1/user/**").build()

    @Bean
    fun actuatorApi(): GroupedOpenApi =
        GroupedOpenApi.builder().displayName("Metric operations").group("actuator").pathsToMatch("/actuator/**").build()

    @Bean
    fun authApi(): GroupedOpenApi =
        GroupedOpenApi.builder().displayName("Auth operations").group("auth").pathsToMatch("/api/v1/auth/**").build()

    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()

    /**
     * We use the PasswordEncoder that is defined in the Spring Security configuration to encode the password.
     * @return -- singleton instance of PasswordEncoder
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun logFilter(): CommonsRequestLoggingFilter =
        CommonsRequestLoggingFilter().apply {
            setIncludeQueryString(true)
            setIncludePayload(true)
            setMaxPayloadLength(10000)
            setIncludeHeaders(true)
            setAfterMessagePrefix("REQUEST DATA: ")
        }

//    @Bean
//    fun customInfoContributor(): InfoContributor {
//        return InfoContributor { builder: org.springframework.boot.actuate.info.Info.Builder ->
//            builder.withDetail("app", mapOf("name" to buildProperties.name, "version" to buildProperties.version))
//        }
//    }

    @Bean
    @Primary
    fun webEndpointProperties(): WebEndpointProperties =
        WebEndpointProperties().also { it: WebEndpointProperties ->
            it.exposure.include = setOf("appHealth") // or use "*" for all
            it.exposure.exclude = setOf("info", "health", "beans", "caches", "conditions", "configprops", "env",
                "flyway", "loggers", "heapdump", "threaddump", "metrics", "sbom", "scheduledtasks", "mappings")
        }

    @Bean
    @Primary
    fun healthEndpointProperties(): HealthEndpointProperties =
        HealthEndpointProperties().also { it: HealthEndpointProperties ->
            it.showComponents = Show.ALWAYS
            it.group["health-group"] = HealthEndpointProperties.Group()
                .also { it.include = mutableSetOf("ping", "diskSpace") }
            //it.group["health-group"]?.include?.add("ping,diskSpace")
            it.logging.slowIndicatorThreshold = Duration.ofSeconds(10)
            it.roles = setOf("ACTUATOR_ADMIN")
        }
}

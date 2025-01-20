package com.github.senocak.util;

import net.javacrumbs.shedlock.support.annotation.Nullable
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.slf4j.Logger
import org.slf4j.event.Level
import org.springframework.aop.Advisor
import org.springframework.aop.aspectj.AspectJExpressionPointcut
import org.springframework.aop.support.AopUtils
import org.springframework.aop.support.DefaultPointcutAdvisor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.support.CronExpression
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.lang.reflect.Method
import java.time.LocalDateTime
import java.util.Enumeration
import kotlin.system.measureTimeMillis


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Log(
    val level: String = "info"
)

@Configuration
class LogConfig(private val logging: Logging){
    /**
     * Creates a Spring AOP advisor that applies the logging aspect to methods annotated with @Log.
     * @return an Advisor that applies the logging aspect to methods annotated with @Log.
     */
    @Bean
    fun logAdvisor(): Advisor {
        val pointcut = AspectJExpressionPointcut()
        pointcut.expression = "@annotation(com.github.senocak.util.Log)"
        return DefaultPointcutAdvisor(pointcut, logging)
    }
}

@Component
class Logging: MethodInterceptor {
    private val log: Logger by logger()
    override fun invoke(invocation: MethodInvocation): Any? {
        val level: String = (findAnnotation(target = invocation.`this`, method = invocation.method, annotationType = Log::class.java)?.level ?: "info").uppercase()
        log.atLevel(Level.valueOf(level))
            .log("method '${invocation.method}' is called on '${invocation.`this`}' with args '${invocation.arguments.joinToString(separator = ",")}'")
        kotlin.runCatching {
            val attr: RequestAttributes = RequestContextHolder.currentRequestAttributes()
            log.atLevel(Level.valueOf(level))
                .log("attr: ${attr.javaClass}, ${attr.sessionId}")
            val serv: ServletRequestAttributes = attr as ServletRequestAttributes
            val en: Enumeration<String> = serv.request.headerNames
            val map: MutableMap<String, String> = mutableMapOf()
            while (en.hasMoreElements()) {
                val k: String = en.nextElement()
                map[k] = serv.request.getHeader(k)
            }
            log.atLevel(Level.valueOf(level)).log("header - $map")
        }.onFailure { it: Throwable ->
            log.atLevel(Level.valueOf(level))
                .log("Error invoking method: ${invocation.method.name}, ${it.message}")
        }
        val next: LocalDateTime? = findAnnotation(target = invocation.`this`, method = invocation.method, annotationType = Scheduled::class.java)
            ?.cron
            ?.let { it: String -> CronExpression.parse(it).next(LocalDateTime.now()) }
        val result: Any?
        val totalTimeMillis: Long = measureTimeMillis { result = invocation.proceed() }
        log.atLevel(Level.valueOf(level))
            .log("${invocation.method.name}: $result, totalTimeMillis: $totalTimeMillis, nextCron: $next")
        return result
    }
}

@Nullable
fun <A : Annotation?> findAnnotation(target: Any?, method: Method, annotationType: Class<A>): A? {
    val annotation: A? = AnnotatedElementUtils.getMergedAnnotation(method, annotationType)
    if (annotation != null)
        return annotation
    // Try to find annotation on proxied class
    val targetClass: Class<*> = AopUtils.getTargetClass(target!!)
    try {
        val methodOnTarget: Method = targetClass.getMethod(method.name, *method.parameterTypes)
        return AnnotatedElementUtils.getMergedAnnotation(methodOnTarget, annotationType)
    } catch (e: NoSuchMethodException) {
        return null
    }
}
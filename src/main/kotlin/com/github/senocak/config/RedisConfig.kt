package com.github.senocak.config

import com.github.senocak.util.logger
import org.slf4j.Logger
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

@Configuration
class RedisConfig(
    private val redisProperties: RedisProperties
){
    private val log: Logger by logger()

    @Bean
    fun jedisPool(): JedisPool {
        log.info("RedisConfig: host=${redisProperties.host}, port=${redisProperties.port}, password=${redisProperties.password}, timeout=${redisProperties.timeout}")
        return JedisPool(JedisPoolConfig(), redisProperties.host, redisProperties.port, redisProperties.timeout.seconds.toInt(),
            if(!redisProperties.password.isNullOrEmpty()) redisProperties.password else null)
    }

    @Bean
    fun jedisConnectionFactory(): LettuceConnectionFactory {
        val redisStandaloneConfiguration = RedisStandaloneConfiguration()
        redisStandaloneConfiguration.hostName = redisProperties.host
        if (!redisProperties.password.isNullOrEmpty())
            redisStandaloneConfiguration.password = RedisPassword.of(redisProperties.password)
        redisStandaloneConfiguration.port = redisProperties.port
        return LettuceConnectionFactory(redisStandaloneConfiguration)
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, Any> =
        RedisTemplate<String, Any>()
            .apply { this.connectionFactory = jedisConnectionFactory() }
}

package com.github.senocak.config

import com.github.senocak.util.logger
import org.slf4j.Logger
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

@Component
@ConfigurationProperties(prefix = "spring.redis")
class RedisConfig {
    lateinit var host: String
    lateinit var port: String
    lateinit var password: String
    lateinit var database: String
    lateinit var timeout: String

    private val log: Logger by logger()

    @Bean
    fun jedisPool(): JedisPool =
        JedisPool(JedisPoolConfig(), host, port.toInt(), timeout.toInt(), password)
            .also {
                log.info("RedisConfig: host=$host, port=$port, password=$password, timeout=$timeout")
            }

    @Bean
    fun lettuceConnectionFactory(): LettuceConnectionFactory {
        val redisStandaloneConfiguration = RedisStandaloneConfiguration()
        redisStandaloneConfiguration.database = database.toInt()
        redisStandaloneConfiguration.hostName = host
        redisStandaloneConfiguration.setPassword(password)
        //redisStandaloneConfiguration.password = password
        redisStandaloneConfiguration.port = port.toInt()
        return LettuceConnectionFactory(redisStandaloneConfiguration)
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> =
        RedisTemplate<String, Any>()
            .apply { this.connectionFactory = connectionFactory }
}

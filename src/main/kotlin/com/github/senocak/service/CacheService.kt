package com.github.senocak.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.senocak.exception.ConfigException
import com.github.senocak.util.AppConstants.Cache
import com.github.senocak.util.logger
import org.slf4j.Logger
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class CacheService(
    private val redisService: RedisService,
    private val objectMapper: ObjectMapper
) {
    private val log: Logger by logger()
    private val hits = AtomicLong(0)
    private val misses = AtomicLong(0)

    /**
     * Get or set cache value with type safety
     */
    fun <T : Any> getOrSet(key: String, clazz: Class<T>, prefix: String = Cache.PREFIX,
                           ttl: Long = Cache.DEFAULT_TTL, loader: () -> T): T {
        val cacheKey = "$prefix$key"
        return try {
            redisService.getValue(key = cacheKey).let { it: String ->
                hits.incrementAndGet()
                log.info("Cache hit for key: $cacheKey")
                objectMapper.readValue(it, clazz)
            }
        } catch (e: ConfigException) {
            misses.incrementAndGet()
            log.error("Cache miss for key: $cacheKey")
            null
        } ?: loader().also { value: T ->
            redisService.setValue(key = cacheKey, value = objectMapper.writeValueAsString(value))
            log.info("Cached value for key: $cacheKey")
        }
    }

    /**
     * Invalidate a specific cache entry
     */
    fun invalidate(key: String, prefix: String = Cache.PREFIX) {
        val cacheKey = "$prefix$key"
        redisService.removeKey(key = cacheKey)
        log.info("Invalidated cache for key: $cacheKey")
    }

    /**
     * Invalidate all cache entries matching a pattern
     */
    fun invalidatePattern(pattern: String) {
        redisService.getKeys(pattern = "$pattern*")
            .forEach { key: String -> invalidate(key = key) }
    }

    /**
     * Get cache statistics
     */
    fun getStats(): Map<String, Long> = mapOf(
        "hits" to hits.get(),
        "misses" to misses.get(),
        "total" to (hits.get() + misses.get()),
        "keyCount" to redisService.getKeys(pattern = "${Cache.PREFIX}*").size.toLong()
    )
}

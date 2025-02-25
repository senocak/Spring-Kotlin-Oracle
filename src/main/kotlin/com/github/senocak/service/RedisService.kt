package com.github.senocak.service

import com.github.senocak.exception.ConfigException
import com.github.senocak.util.logger
import org.slf4j.Logger
import org.springframework.stereotype.Service
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub

@Service
class RedisService(
    private val jedisPool: JedisPool
) {
    private val log: Logger by logger()

    @Throws(ConfigException::class)
    fun ping(): String {
        val jedis: Jedis = jedisPool.resource
        return try {
            jedis.ping()
        } catch (e: Exception) {
            log.error("Error pinging: ${e.message}")
            throw ConfigException(t = e)
        } finally {
            releaseJedis(jedis = jedis)
        }
    }

    @Throws(ConfigException::class)
    fun jedisPool(): Map<String, Any> = mapOf(
        "numWaiters" to jedisPool.numWaiters,
        "numActive" to jedisPool.numActive,
        "numIdle" to jedisPool.numIdle,
        "meanBorrowWaitTimeMillis" to jedisPool.meanBorrowWaitTimeMillis,
        "maxBorrowWaitTimeMillis" to jedisPool.maxBorrowWaitTimeMillis,
        "isClosed" to jedisPool.isClosed,
    )

    @Throws(ConfigException::class)
    fun getValue(key: String?): String {
        val jedis: Jedis = jedisPool.resource
        return try {
            jedis[key]
        } catch (e: Exception) {
            log.error("Error getting value for key: $key, ${e.message}")
            throw ConfigException(t = e)
        } finally {
            releaseJedis(jedis = jedis)
        }
    }

    @Throws(ConfigException::class)
    fun setValue(key: String?, value: String?) {
        val jedis: Jedis = jedisPool.resource
        try {
            jedis[key] = value
        } catch (e: Exception) {
            log.error("Error setting value for key: $key, ${e.message}")
            throw ConfigException(t = e)
        } finally {
            releaseJedis(jedis = jedis)
        }
    }

    @Throws(ConfigException::class)
    fun getAttribute(key: String?, attributeKey: String?): String {
        val jedis: Jedis = jedisPool.resource
        return try {
            jedis.hget(key, attributeKey)
        } catch (e: Exception) {
            log.error("Error getting attribute for key: $key and attributeKey: $attributeKey, ${e.message}")
            throw ConfigException(t = e)
        } finally {
            releaseJedis(jedis = jedis)
        }
    }

    @Throws(ConfigException::class)
    fun getAttributes(key: String, attributeKeys: List<String>): List<String> {
        val jedis: Jedis = jedisPool.resource
        return try {
            val attributeKeyArray: Array<String?> = arrayOfNulls(size = attributeKeys.size)
            jedis.hmget(key, *attributeKeyArray)
        } catch (e: Exception) {
            log.error("Error getting attributes for key: $key and attributeKeys: $attributeKeys, ${e.message}")
            throw ConfigException(t = e)
        } finally {
            releaseJedis(jedis = jedis)
        }
    }

    @Throws(ConfigException::class)
    fun setAttribute(key: String?, attributeKey: String?, value: String?) {
        val jedis: Jedis = jedisPool.resource
        try {
            jedis.hset(key, attributeKey, value)
        } catch (e: Exception) {
            log.error("Error setting attribute for key: $key, attributeKey: $attributeKey, value: $value, ${e.message}")
            throw ConfigException(t = e)
        } finally {
            releaseJedis(jedis = jedis)
        }
    }

    @Throws(ConfigException::class)
    fun getAttributes(key: String?): Map<String, String> {
        val jedis: Jedis = jedisPool.resource
        return try {
            jedis.hgetAll(key)
        } catch (e: Exception) {
            log.error("Error getting attributes for key: $key, ${e.message}")
            throw ConfigException(t = e)
        } finally {
            releaseJedis(jedis = jedis)
        }
    }

    @Throws(ConfigException::class)
    fun setAttributes(key: String?, attributeMap: Map<String?, String?>?) {
        val jedis: Jedis = jedisPool.resource
        try {
            jedis.hmset(key, attributeMap)
        } catch (e: Exception) {
            log.error("Error setting attributes for key: $key, attributeMap: $attributeMap, ${e.message}")
            throw ConfigException(t = e)
        } finally {
            releaseJedis(jedis = jedis)
        }
    }

    @Throws(ConfigException::class)
    fun removeAttributes(key: String?, vararg attributeKeys: String?) {
        val jedis: Jedis = jedisPool.resource
        try {
            jedis.hdel(key, *attributeKeys)
        } catch (e: Exception) {
            log.error("Error removing attributes for key: $key, attributeKeys: $attributeKeys, ${e.message}")
            throw ConfigException(t = e)
        } finally {
            releaseJedis(jedis = jedis)
        }
    }

    @Throws(ConfigException::class)
    fun removeKey(key: String?) {
        val jedis: Jedis = jedisPool.resource
        try {
            jedis.del(key)
        } catch (e: Exception) {
            log.error("Error removing key: $key, ${e.message}")
            throw ConfigException(t = e)
        } finally {
            releaseJedis(jedis = jedis)
        }
    }

    @Throws(ConfigException::class)
    fun getKeys(pattern: String?): Set<String> {
        val jedis: Jedis = jedisPool.resource
        return try {
            jedis.keys(pattern)
        } catch (e: Exception) {
            log.error("Error getting keys for pattern: $pattern, ${e.message}")
            throw ConfigException(t = e)
        } finally {
            releaseJedis(jedis = jedis)
        }
    }

    @Throws(ConfigException::class)
    fun psubscribe(subscriber: JedisPubSub?, vararg channels: String?) {
        val jedis: Jedis = jedisPool.resource
        try {
            Thread {
                try {
                    jedis.psubscribe(subscriber, *channels)
                } finally {
                    releaseJedis(jedis = jedis)
                }
            }.start()
        } catch (e: Exception) {
            log.error("Error psubscribing to channels: $channels, ${e.message}")
            releaseJedis(jedis = jedis)
            throw ConfigException(t = e)
        }
    }

    @Throws(ConfigException::class)
    fun addToList(key: String?, value: String?) {
        val jedis: Jedis = jedisPool.resource
        try {
            jedis.lpush(key, value)
        } catch (e: Exception) {
            log.error("Error adding value to list for key: $key, value: $value, ${e.message}")
            throw ConfigException(t = e)
        } finally {
            releaseJedis(jedis = jedis)
        }
    }

    @Throws(ConfigException::class)
    fun getFromList(key: String?, start: Int, end: Int): List<String> {
        val jedis: Jedis = jedisPool.resource
        return try {
            jedis.lrange(key, start.toLong(), end.toLong())
        } catch (e: Exception) {
            log.error("Error getting values from list for key: $key, start: $start, end: $end, ${e.message}")
            throw ConfigException(t = e)
        } finally {
            releaseJedis(jedis = jedis)
        }
    }

    @Throws(ConfigException::class)
    fun removeFromList(key: String?, count: Long, value: String?): Long {
        val jedis: Jedis = jedisPool.resource
        return try {
            jedis.lrem(key, count, value)
        } catch (e: Exception) {
            log.error("Error removing value from list for key: $key, count: $count, value: $value, ${e.message}")
            throw ConfigException(t = e)
        } finally {
            releaseJedis(jedis = jedis)
        }
    }

    @Throws(ConfigException::class)
    fun getAllValuesForPattern(pattern: String): Map<String, String> {
        val jedis: Jedis = jedisPool.resource
        return try {
            jedis.keys("$pattern:*").associateWith { key: String -> jedis.get(key) }
        } catch (e: Exception) {
            log.error("Error getting all values for pattern: $pattern, ${e.message}")
            throw ConfigException(t = e)
        } finally {
            releaseJedis(jedis = jedis)
        }
    }

    /**
     * release jedis resource
     * @param jedis jedis
     */
    private fun releaseJedis(jedis: Jedis?) {
        jedis?.close()
    }
}

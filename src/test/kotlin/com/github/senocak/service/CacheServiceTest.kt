package com.github.senocak.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.senocak.domain.User
import com.github.senocak.exception.ConfigException
import com.github.senocak.util.AppConstants.Cache
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockitoExtension::class)
class CacheServiceTest {

    @Mock
    private lateinit var redisService: RedisService

    @Mock
    private lateinit var objectMapper: ObjectMapper

    private lateinit var cacheService: CacheService

    @BeforeEach
    fun setup() {
        cacheService = CacheService(redisService, objectMapper)
    }

    @Test
    fun `should return cached value when exists`() {
        // given
        val key = "test-key"
        val cachedJson = """{"name":"Test User"}"""
        val expectedUser = User(name = "Test User")

        `when`(redisService.getValue("${Cache.PREFIX}$key")).thenReturn(cachedJson)
        `when`(objectMapper.readValue(cachedJson, User::class.java)).thenReturn(expectedUser)

        // when
        val result = cacheService.getOrSet(key, User::class.java) { 
            throw RuntimeException("Loader should not be called") 
        }

        // then
        assertEquals(expectedUser, result)
        verify(redisService).getValue("${Cache.PREFIX}$key")

        // Verify stats
        val stats = cacheService.getStats()
        assertEquals(1, stats["hits"])
        assertEquals(0, stats["misses"])
    }

    @Test
    fun `should use loader when cache miss`() {
        // given
        val key = "test-key"
        val user = User(name = "Test User")
        val userJson = """{"name":"Test User"}"""

        `when`(redisService.getValue("${Cache.PREFIX}$key")).thenThrow(ConfigException(RuntimeException("Cache miss")))
        `when`(objectMapper.writeValueAsString(user)).thenReturn(userJson)

        // when
        val result = cacheService.getOrSet(key, User::class.java) { user }

        // then
        assertEquals(user, result)
        verify(redisService).setValue("${Cache.PREFIX}$key", userJson)

        // Verify stats
        val stats = cacheService.getStats()
        assertEquals(0, stats["hits"])
        assertEquals(1, stats["misses"])
    }

    @Test
    fun `should invalidate cache`() {
        // given
        val key = "test-key"

        // when
        cacheService.invalidate(key)

        // then
        verify(redisService).removeKey("${Cache.PREFIX}$key")
    }

    @Test
    fun `should invalidate pattern`() {
        // given
        val pattern = "test*"
        val keys = setOf("test1", "test2")

        `when`(redisService.getKeys("$pattern*")).thenReturn(keys)

        // when
        cacheService.invalidatePattern(pattern)

        // then
        keys.forEach { key ->
            verify(redisService).removeKey(key)
        }
    }
}

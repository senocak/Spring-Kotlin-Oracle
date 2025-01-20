package com.github.senocak.controller

import com.github.senocak.config.LockConfig
import com.github.senocak.service.RedisService
import com.github.senocak.util.logger
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.annotation.PostConstruct
import net.javacrumbs.shedlock.core.LockAssert
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import redis.clients.jedis.JedisPool

@RestController
@RequestMapping(BaseController.V1_PUBLIC_URL)
@Tag(name = "Shared", description = "Shared Controller")
class SharedController(
    private val redisService: RedisService,
    private val jedisPool: JedisPool,
    private val jdbcTemplate: JdbcTemplate,
): BaseController() {
    private val log: Logger by logger()

    @GetMapping(path=["/ping"])
    fun isAlive(): String = "ping"

    @GetMapping(path=["/jedis/ping"])
    fun jedisPing(): String = jedisPool.resource.ping()

    @GetMapping(path=["/jedis/schedulers"])
    fun jedisSchedulers(): Map<String, String> =
        redisService.getAllValuesForPattern(pattern = "job-lock:${LockConfig.ENV}")

    @GetMapping(path=["/lock"])
    fun assertLocked(): String = LockAssert.assertLocked().runCatching { "locked" }.getOrElse { "not locked" }

    @PostConstruct
    fun init() {
        /*
        create table bank_transfer(id VARCHAR2(50 char) primary key);
        create table bank_transfer_uuid(id RAW(16) primary key);
        create table bank_transfer_uuid_v7(id RAW(16) primary key);

        val measureTimeMillis1: Long = measureTimeMillis {
            for (i: Int in 1..100) {
                jdbcTemplate.batchUpdate(
                    "insert into BANK_TRANSFER (id) values (?)",
                    object : BatchPreparedStatementSetter {
                        @Throws(SQLException::class)
                        override fun setValues(ps: PreparedStatement, i: Int) {
                            ps.setString(1, UUID.randomUUID().toString())
                        }
                        override fun getBatchSize(): Int = 100_000
                    })
                log.info("Inserted $i * 100_000 records")
            }
        }
        log.info("measureTimeMillis1: $measureTimeMillis1") // 5469370
        val measureTimeMillis2: Long = measureTimeMillis {
            for (i: Int in 1..100) {
                jdbcTemplate.batchUpdate("insert into BANK_TRANSFER_UUID (id) values (?)",
                    object : BatchPreparedStatementSetter {
                        @Throws(SQLException::class)
                        override fun setValues(ps: PreparedStatement, i: Int) {
                            ps.setBytes(1, UUID.randomUUID().toString().toByteArray(Charsets.UTF_8).copyOf(16))
                        }
                        override fun getBatchSize(): Int = 100_000
                    })
                log.info("Inserted $i * 100_000 records")
            }
        }
        val measureTimeMillis3: Long = measureTimeMillis {
            for (i: Int in 1..100) {
                jdbcTemplate.batchUpdate(
                    "insert into BANK_TRANSFER_UUID_V7 (id) values (?)",
                    object : BatchPreparedStatementSetter {
                        @Throws(SQLException::class)
                        override fun setValues(ps: PreparedStatement, i: Int) {
                            ps.setString(1, Generators.timeBasedEpochGenerator().generate().toString())
                        }
                        override fun getBatchSize(): Int = 100_000
                    })
                log.info("Inserted $i * 100_000 records")
            }
        }
        log.info("measureTimeMillis3: $measureTimeMillis3")
        */
        /*
        SELECT
        t.table_name AS "table",
        i.index_name AS "index",
        TO_CHAR(SUM(s.bytes) / 1024 / 1024, 'FM999,999,999') || ' MB' AS "table size",
        TO_CHAR(SUM(si.bytes) / 1024 / 1024, 'FM999,999,999') || ' MB' AS "index size"
        FROM
        user_tables t
                JOIN
        user_indexes i ON t.table_name = i.table_name
                LEFT JOIN
                user_segments s ON t.table_name = s.segment_name AND s.segment_type = 'TABLE'
        LEFT JOIN
                user_segments si ON i.index_name = si.segment_name AND si.segment_type = 'INDEX'
        WHERE
        t.table_name NOT LIKE 'BIN$%'  -- Exclude Oracle's recycled tables
        GROUP BY
                t.table_name, i.index_name;
        */
    }
}

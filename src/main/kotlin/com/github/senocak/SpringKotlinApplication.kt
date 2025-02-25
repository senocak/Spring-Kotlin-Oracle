package com.github.senocak

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringKotlinApplication

fun main(args: Array<String>) {
    runApplication<SpringKotlinApplication>(*args)
}

//@PostConstruct
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
package com.github.senocak.config

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory

@Configuration
class LockConfig {

    @Bean
    fun lockProvider(connectionFactory: RedisConnectionFactory): LockProvider =
        RedisLockProvider(connectionFactory, ENV)


//    @Bean
//    fun lockProvider(dataSource: DataSource): LockProvider =
//        JdbcTemplateLockProvider(
//                JdbcTemplateLockProvider.Configuration.builder()
//                    .withTableName("shedlock")
//                    .withColumnNames(JdbcTemplateLockProvider.ColumnNames("name", "lock_until", "locked_at", "locked_by"))
//                    .withLockedByValue("my-value")
//                    .withDbUpperCase(true)
//                    .withJdbcTemplate(JdbcTemplate(dataSource))
//                    //.withIsolationLevel(3)
//                    .usingDbTime() // the lock provider will use UTC time based on the DB server clock
//                    .build()
//                )

    companion object {
        const val ENV = "ENV1"
    }
}

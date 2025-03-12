package com.github.senocak.service

import com.github.senocak.domain.Role
import com.github.senocak.domain.User
import com.github.senocak.domain.UserRepository
import com.github.senocak.exception.ServerException
import com.github.senocak.util.RoleName
import com.github.senocak.util.logger
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.springframework.jdbc.core.support.JdbcDaoSupport
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import javax.sql.DataSource

@Service
class UserService(
    private val userRepository: UserRepository,
    private val dataSource: DataSource,
    private val cacheService: CacheService
): UserDetailsService, JdbcDaoSupport() {
    private val log: Logger by logger()

    companion object {
        private const val USER_CACHE_KEY = "user:"
    }

    @PostConstruct
    fun initialize() {
        setDataSource(dataSource)
        log.info("Datasource used: $dataSource")
    }

    fun findAll(): MutableIterable<User> = userRepository.findAll()

    /**
     * @param email -- string email to find in db
     * @return -- true or false
     */
    fun existsByEmail(email: String): Boolean =
        userRepository.existsByEmail(email = email)

    /**
     * @param email -- string email to find in db
     * @return -- User object
     * @throws UsernameNotFoundException -- throws UsernameNotFoundException
     */
    @Throws(UsernameNotFoundException::class)
    fun findByEmail(email: String): User =
        cacheService.getOrSet(key = email, prefix = USER_CACHE_KEY, clazz = User::class.java) {
            userRepository.findByEmail(email = email) ?: throw UsernameNotFoundException("user_not_found")
        }

    /**
     * @param user -- User object to persist to db
     * @return -- User object that is persisted to db
     */
    fun save(user: User): User {
        val savedUser: User = userRepository.save(user)
        // Invalidate cache for the user's email
        user.email?.let { email ->
            cacheService.invalidate(key = email, prefix = USER_CACHE_KEY)
        }
        return savedUser
    }

    fun deleteAllUsers() {
        userRepository.deleteAll()
        // Clear all user caches
        cacheService.invalidatePattern(pattern = "${USER_CACHE_KEY}*")
    }

    /**
     * @param email -- id
     * @return -- Spring User object
     */
    @Transactional
    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(email: String): org.springframework.security.core.userdetails.User {
        val user: User = findByEmail(email = email)
        val authorities: List<GrantedAuthority> = user.roles
            .map { r: Role -> SimpleGrantedAuthority(RoleName.fromString(r = r.name.toString()).name) }
            .toList()
        return org.springframework.security.core.userdetails.User(user.email, user.password, authorities)
    }

    /**
     * @return -- User entity that is retrieved from db
     * @throws ServerException -- throws ServerException
     */
    @Throws(ServerException::class)
    fun loggedInUser(): User =
        (SecurityContextHolder.getContext().authentication.principal as org.springframework.security.core.userdetails.User).username
            .run { findByEmail(email = this) }

    val allUsers: List<Any>
        get() {
            val sql = "SELECT name, email, password FROM users"
            return jdbcTemplate!!.query<User>(sql) { rs: ResultSet, rowNum: Int ->
                User(
                    name = rs.getString("name"),
                    email = rs.getString("email"),
                    password = rs.getString("password")
                )
            }
        }
}

package com.github.senocak.service

import com.github.senocak.domain.Role
import com.github.senocak.domain.User
import com.github.senocak.domain.UserRepository
import com.github.senocak.exception.ServerException
import com.github.senocak.util.RoleName
import jakarta.annotation.PostConstruct
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
    private val dataSource: DataSource
): UserDetailsService, JdbcDaoSupport() {

    @PostConstruct
    fun initialize() {
        setDataSource(dataSource)
        println("Datasource used: $dataSource")
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
        userRepository.findByEmail(email = email) ?: throw UsernameNotFoundException("user_not_found")

    /**
     * @param user -- User object to persist to db
     * @return -- User object that is persisted to db
     */
    fun save(user: User): User = userRepository.save(user)

    fun deleteAllUsers() = userRepository.deleteAll()

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
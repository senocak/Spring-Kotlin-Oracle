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
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

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

    fun getUsersWithPagination(page: Int, size: Int, name: String?, email: String?, roleIds: List<String>?,
                               startDate: String?, endDate: String?, operator: String? = "AND"): Page<User> {
        val (whereClause: String, params: MutableList<Any>) = buildWhereClause(name = name, email = email, roleIds = roleIds, startDate = startDate,
            endDate = endDate, operator = operator)
        // Build the count query
        val countSql = "SELECT COUNT(DISTINCT u.id) FROM users u LEFT JOIN user_roles ur ON u.id = ur.user_id $whereClause"
        log.info("Sql statement for count: $countSql")
        val totalElements = jdbcTemplate!!.queryForObject(countSql, params.toTypedArray(), Long::class.java) ?: 0

        // Build the main query with pagination
        val sql: String = """
            SELECT DISTINCT u.id, u.name, u.email, u.password, u.created_at, u.updated_at, u.last_name,
                   r.name as role_name
            FROM users u 
            LEFT JOIN user_roles ur ON u.id = ur.user_id 
            LEFT JOIN roles r ON ur.role_id = r.id
            $whereClause 
            ORDER BY u.created_at DESC
            OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
        """.trimIndent()
        params.add(element = page * size)   // OFFSET parameter
        params.add(element = size)  // FETCH NEXT parameter

        // Create a map to store users and their roles
        val userMap: MutableMap<String, User> = mutableMapOf()
        jdbcTemplate!!.query(sql, params.toTypedArray()) { rs: ResultSet, _: Int ->
            val userId: String = rs.getString("id")
            val user: User = userMap.getOrPut(key = userId) {
                User(
                    name = rs.getString("name"),
                    email = rs.getString("email"),
                    password = rs.getString("password")
                ).apply {
                    id = userId
                    createdAt = rs.getTimestamp("created_at")
                    updatedAt = rs.getTimestamp("updated_at")
                    lastName = rs.getString("last_name")
                    roles = mutableListOf()
                }
            }
            // Add role if it exists
            rs.getString("role_name")?.let { roleName: String ->
                val role = Role(name = RoleName.valueOf(roleName))
                if (!user.roles.any { it.name == role.name })
                    (user.roles as MutableList<Role>).add(element = role)
            }
        }
        return PageImpl(userMap.values.toList(), PageRequest.of(page, size), totalElements)
    }

    private fun buildWhereClause(name: String?, email: String?, roleIds: List<String>?, startDate: String?,
                                 endDate: String?, operator: String? = "AND"): Pair<String, MutableList<Any>> {
        val conditions: MutableList<String> = mutableListOf()
        if (!name.isNullOrBlank())
            conditions.add("LOWER(u.name) LIKE LOWER(CONCAT('%', ?, '%'))")
        if (!email.isNullOrBlank())
            conditions.add("LOWER(u.email) LIKE LOWER(CONCAT('%', ?, '%'))")
        if (!roleIds.isNullOrEmpty())
            conditions.add("ur.role_id IN (${roleIds.joinToString(separator = ",") { "?" }})")
        if (!startDate.isNullOrBlank())
            conditions.add("u.created_at >= TO_TIMESTAMP(?, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"')")
        if (!endDate.isNullOrBlank())
            conditions.add("u.created_at <= TO_TIMESTAMP(?, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"')")
        val sql: String =  if (conditions.isEmpty()) "" else "WHERE ${conditions.joinToString(separator = " $operator ")}"
        val params: MutableList<Any> = mutableListOf()
        // Add parameters for WHERE clause
        name?.let { params.add(element = it) }
        email?.let { params.add(element = it) }
        roleIds?.forEach { params.add(element = it) }
        startDate?.let { params.add(element = it) }
        endDate?.let { params.add(element = it) }
        return sql to params
    }
}

package com.github.senocak.domain

import com.github.senocak.util.RoleName
import jakarta.persistence.EntityManager
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import java.util.UUID
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service

interface RoleRepository: CrudRepository<Role, UUID> {
    fun findByName(roleName: RoleName): Role?
}
interface UserRepository: CrudRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
}
@Service
class UserRepositoryCls(entityManager: EntityManager): SimpleJpaRepository<User, UUID>(User::class.java, entityManager)
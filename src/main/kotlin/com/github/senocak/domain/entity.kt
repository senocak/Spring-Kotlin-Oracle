package com.github.senocak.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.senocak.util.RoleName
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.IdGeneratorType
import java.io.Serializable
import java.util.Date
import java.util.UUID


@MappedSuperclass
open class BaseDomain(
    @Id
    //@GeneratedValue(generator = "UUID")
    @GeneratedValue(strategy = GenerationType.UUID)
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    var id: UUID? = null,
    @Column var createdAt: Date = Date(),
    @Column var updatedAt: Date = Date()
) : Serializable

@Entity
@Table(name = "users", uniqueConstraints = [UniqueConstraint(columnNames = ["email"])])
data class User(
    @Column(name = "name", nullable = false, length = 50) var name: String? = null,
    @Column(name = "email", nullable = false, length = 100) var email: String? = null,
    @Column(name = "password", nullable = false) var password: String? = null
) : BaseDomain() {
    @JoinTable(name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id", nullable = false, foreignKey = ForeignKey(name = "fk_user_roles_user_id"))],
        inverseJoinColumns = [JoinColumn(
            name = "role_id",
            nullable = false,
            foreignKey = ForeignKey(name = "fk_user_roles_role_id")
        )],
        uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "role_id"], name = "uk_user_roles_unique_user_id_role_id")]
    )
    @ManyToMany(fetch = FetchType.EAGER)
    var roles: List<Role> = arrayListOf()

    @Column(name = "last_name", nullable = true, length = 50)
    var lastName: String? = null
}

@Entity
@Table(name = "roles")
data class Role(@Column @Enumerated(EnumType.STRING) var name: RoleName? = null): BaseDomain()

//@Entity
//@Table(name = "shedlock")
//class Shedlock {
//    @Id
//    @Column(name = "name", length = 64, nullable = false)
//    var name: String? = null
//
//    @Column(name = "lock_until", nullable = false)
//    var lockUntil: Timestamp? = null
//
//    @Column(name = "locked_at", nullable = false)
//    var lockedAt: Timestamp? = null
//
//    @Column(name = "locked_by", length = 255, nullable = false)
//    var lockedBy: String? = null
//}
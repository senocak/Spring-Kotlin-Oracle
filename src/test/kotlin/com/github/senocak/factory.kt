package com.github.senocak

import com.github.senocak.TestConstants.USER_EMAIL
import com.github.senocak.TestConstants.USER_NAME
import com.github.senocak.TestConstants.USER_PASSWORD
import com.github.senocak.domain.Role
import com.github.senocak.domain.User
import com.github.senocak.util.RoleName
import java.util.ArrayList
import java.util.UUID

fun createTestUser(): User =
    User(name = USER_NAME, email = USER_EMAIL, password = USER_PASSWORD)
        .also { it: User ->
            it.id = UUID.randomUUID().toString()
            it.roles = arrayListOf<Role>()
                .also { list: ArrayList<Role> -> list.add(element = createRole(RoleName.ROLE_USER)) }
                .also { list: ArrayList<Role> -> list.add(element = createRole(RoleName.ROLE_ADMIN)) }
        }

fun createRole(role: RoleName? = RoleName.ROLE_USER): Role =
    Role(name = role)
        .also { it: Role -> it.id = UUID.randomUUID().toString() }
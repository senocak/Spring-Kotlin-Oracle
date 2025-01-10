package com.github.senocak

import com.github.senocak.TestConstants.USER_EMAIL
import com.github.senocak.TestConstants.USER_NAME
import com.github.senocak.TestConstants.USER_PASSWORD
import com.github.senocak.domain.TodoItem
import com.github.senocak.domain.User
import com.github.senocak.util.RoleName
import java.util.ArrayList
import java.util.Date
import java.util.UUID

fun createTestUser(): User =
    User(name = USER_NAME, email = USER_EMAIL, password = USER_PASSWORD)
        .also {
            it.id = UUID.randomUUID()
            it.roles = arrayListOf<String>()
                .also { list: ArrayList<String> -> list.add(element = RoleName.ROLE_USER.role) }
                .also { list: ArrayList<String> -> list.add(element = RoleName.ROLE_ADMIN.role) }
            it.emailActivatedAt = Date()
        }

fun createTestTodo(): TodoItem =
    TodoItem(description = "description", owner = UUID.randomUUID(), finished = false)
        .also {
            it.id = UUID.randomUUID()
        }
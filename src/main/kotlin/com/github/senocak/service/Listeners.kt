package com.github.senocak.service

import com.github.senocak.config.OracleConfiguration
import com.github.senocak.domain.Role
import com.github.senocak.domain.RoleRepository
import com.github.senocak.domain.User
import com.github.senocak.util.RoleName
import com.github.senocak.util.logger
import org.slf4j.Logger
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.Profiles
import org.springframework.scheduling.annotation.Async
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
@Async
class Listeners(
    private val dataSourceConfig: OracleConfiguration,
    private val userService: UserService,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder
){
    private val log: Logger by logger()

    // FIXME: @Profile("!integration-test")
    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReadyEvent(event: ApplicationReadyEvent) {
        if (event.applicationContext.environment.acceptsProfiles(Profiles.of("integration-test"))) {
            log.warn("Integration test profile is active")
            return
        }
        if (dataSourceConfig.ddl == "create" || dataSourceConfig.ddl == "create-drop") {
            val roles: List<Role> = createRoles()
            User(name = "anil1", email = "anil1@senocak.com", password = passwordEncoder.encode("asenocak"))
                .also { u: User ->
                    u.roles = listOf(roles[1], roles[0])
                    u.lastName = "Senocak1"
                }
                .run {
                    userService.save(user = this)
                }
            User(name = "anil2", email = "anil2@gmail.com", password = passwordEncoder.encode("asenocak"))
                .also { u: User ->
                    u.roles = listOf(element = roles[1])
                    u.lastName = "Senocak2"
                }
                .run {
                    userService.save(user = this)
                }
            log.info("Seeding completed")
        }
    }

    /**
     * Creates and saves a list of roles in the repository. This function initializes a list of roles with `ROLE_ADMIN`
     * and `ROLE_USER` saves them to the `roleRepository`, and returns the list of saved roles.
     * @return List of created and saved roles.
     */
    private fun createRoles(): List<Role> =
        arrayListOf<Role>()
            .also { roleList: ArrayList<Role> ->
                roleList.add(Role().also { it.name = RoleName.ROLE_ADMIN })
                roleList.add(Role().also { it.name = RoleName.ROLE_USER })
            }
            .apply { roleRepository.saveAll(this) }
}

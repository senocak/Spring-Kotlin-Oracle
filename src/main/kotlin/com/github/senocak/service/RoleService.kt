package com.github.senocak.service

import com.github.senocak.domain.Role
import com.github.senocak.domain.RoleRepository
import com.github.senocak.exception.ServerException
import com.github.senocak.util.OmaErrorMessageType
import com.github.senocak.util.RoleName
import com.github.senocak.util.logger
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class RoleService(
    private val roleRepository: RoleRepository,
) {
    private val log: Logger by logger()

    /**
     * @param roleName -- enum variable to retrieve from db
     * @return -- Role object retrieved from db
     */
    fun findByName(roleName: RoleName): Role = roleRepository.findByName(roleName = roleName)
        ?: throw ServerException(omaErrorMessageType = OmaErrorMessageType.MANDATORY_INPUT_MISSING,
            variables = arrayOf("role_not_found"), statusCode = HttpStatus.NOT_FOUND)
            .also { log.error("User Role is not found") }
}

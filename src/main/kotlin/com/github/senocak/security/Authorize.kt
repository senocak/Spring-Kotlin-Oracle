package com.github.senocak.security

import com.github.senocak.util.AppConstants.ADMIN
import com.github.senocak.util.AppConstants.USER

@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION
)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class Authorize(
    val roles: Array<String> = [ADMIN, USER]
)
package com.saveourtool.save.utils

/**
 * Annotation to mark field to hide them from toString in data class
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
annotation class Secret

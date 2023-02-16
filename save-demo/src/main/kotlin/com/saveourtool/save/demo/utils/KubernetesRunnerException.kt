package com.saveourtool.save.demo.utils

/**
 * Exception is thrown on kubernetes internal exceptions
 */
class KubernetesRunnerException(message: String, cause: Throwable? = null) : Exception(message, cause)

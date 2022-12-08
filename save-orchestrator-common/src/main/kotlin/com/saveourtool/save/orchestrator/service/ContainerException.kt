package com.saveourtool.save.orchestrator.service

/**
 * Wrapper for exception from Docker or Kubernetes
 *
 * @property message
 * @property cause
 */
class ContainerException(
    override val message: String,
    cause: Throwable
) : RuntimeException(message, cause)

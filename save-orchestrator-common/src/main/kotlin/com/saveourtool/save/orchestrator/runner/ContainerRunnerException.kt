package com.saveourtool.save.orchestrator.runner

/**
 * Base exception during interaction with an engine responsible for running save-agents
 */
class ContainerRunnerException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

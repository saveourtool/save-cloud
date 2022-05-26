package com.saveourtool.save.orchestrator.docker

/**
 * Base exception during interaction with an engine responsible for running save-agents
 */
class AgentRunnerException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

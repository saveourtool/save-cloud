package com.saveourtool.save.orchestrator.utils

import org.slf4j.Logger

interface LoggingContext {
    val logger: Logger
}

class LoggingContextImpl(override val logger: Logger) : LoggingContext

/**
 * Support for logging using context receivers
 */

package com.saveourtool.save.orchestrator.utils

import org.slf4j.Logger

/**
 * Context receiver to provide an instance of [Logger]
 *
 * **KEEP:** [Context receivers](https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md)
 */
interface LoggingContext {
    /**
     * And instance of [Logger]
     */
    val logger: Logger
}

/**
 * Simple wrapper that implements [LoggingContext]
 * @property logger
 */
class LoggingContextImpl(override val logger: Logger) : LoggingContext

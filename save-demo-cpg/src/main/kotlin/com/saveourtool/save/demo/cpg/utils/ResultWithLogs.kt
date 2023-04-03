package com.saveourtool.save.demo.cpg.utils

import arrow.core.Either

/**
 * @property result contains a result or exception caught on getting the result
 * @property logs
 */
data class ResultWithLogs<R>(
    val result: Either<Throwable, R>,
    val logs: List<String>,
)

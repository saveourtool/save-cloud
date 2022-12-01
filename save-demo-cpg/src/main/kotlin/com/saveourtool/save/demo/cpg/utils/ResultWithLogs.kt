package com.saveourtool.save.demo.cpg.utils

/**
 * @property result
 * @property logs
 */
data class ResultWithLogs<R>(
    val result: R,
    val logs: List<String>,
)

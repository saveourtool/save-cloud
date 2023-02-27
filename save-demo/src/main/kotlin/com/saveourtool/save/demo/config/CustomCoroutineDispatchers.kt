package com.saveourtool.save.demo.config

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Custom [CoroutineDispatcher] used by application
 *
 * @property default [CoroutineDispatcher] for default operations
 * @property io [CoroutineDispatcher] for IO operations
 */
class CustomCoroutineDispatchers(
    val default: CoroutineDispatcher = Dispatchers.Default,
    val io: CoroutineDispatcher = Dispatchers.IO,
)
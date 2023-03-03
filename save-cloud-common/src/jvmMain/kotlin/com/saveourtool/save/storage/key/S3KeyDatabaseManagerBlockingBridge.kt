package com.saveourtool.save.storage.key

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

/**
 * A bridge for [AbstractS3KeyDatabaseManager] for blocking (IO) operations
 *
 * @property ioScheduler
 * @property ioDispatcher
 */
@Component
@ConditionalOnBean
class S3KeyDatabaseManagerBlockingBridge(
    val ioScheduler: Scheduler = Schedulers.boundedElastic(),
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
)

/**
 * Utilities for [Storage]
 */

package com.saveourtool.save.storage

import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.info
import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * A dedicated Scheduler for async operations in storage
 */
val scheduler = Schedulers.boundedElastic()

/**
 * @param repository repository for [E] to check that corresponded [E] exists
 * @return [Flux] with unexpected ids
 */
fun <S : Storage<Long>, E : BaseEntity> S.detectAsyncUnexpectedIds(
    repository: BaseEntityRepository<E>,
): Flux<Long> = list()
    .filterWhen { id -> blockingToMono { repository.findById(id).isEmpty } }

/**
 * @param repository repository for [E] to check that corresponded [E] exists
 * @param log informs about unexpected ids after their deletion
 * @return [Mono] without body
 */
inline fun <S : Storage<Long>, reified E : BaseEntity> S.deleteAsyncUnexpectedIds(
    repository: BaseEntityRepository<E>,
    log: Logger,
): Mono<Unit> = detectAsyncUnexpectedIds(repository)
    .flatMap { unexpectedId -> delete(unexpectedId).map { unexpectedId } }
    .collectList()
    .filter { it.isNotEmpty() }
    .map { unexpectedIds ->
        log.info {
            "Deleted unexpected ids $unexpectedIds from storage ${this::class.simpleName} since associated ${E::class.simpleName} don't exist"
        }
    }
    .defaultIfEmpty(Unit)

/**
 * Utilities for [Storage] which implements [AbstractS3Storage]
 */

package com.saveourtool.save.storage

import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.info
import org.slf4j.Logger
import reactor.core.publisher.Mono

/**
 * Delimiter for S3 key
 */
const val PATH_DELIMITER = "/"

/**
 * @param prefix should not end with [PATH_DELIMITER] -- will be deleted
 * @param suffix should not start with [PATH_DELIMITER] -- will be deleted
 * @return a s3 key by concat [prefix] and [suffix] and a single [PATH_DELIMITER] between them
 */
fun concatS3Key(prefix: String, suffix: String): String =
        "${prefix.removeSuffix(PATH_DELIMITER)}$PATH_DELIMITER${suffix.removePrefix(PATH_DELIMITER)}"

/**
 * @param repository repository for [E] to check that corresponded [E] exists
 * @param log informs about unexpected ids after their deletion
 * @return [Mono] without body
 */
inline fun <S : AbstractS3Storage<Long>, reified E: BaseEntity> S.deleteAsyncUnexpectedIds(
    repository: BaseEntityRepository<E>,
    log: Logger,
): Mono<Unit> = list()
    .filterWhen { id -> blockingToMono { repository.findById(id).isEmpty } }
    .flatMap { unexpectedId -> delete(unexpectedId).map { unexpectedId } }
    .collectList()
    .filter { it.isNotEmpty() }
    .map { unexpectedIds ->
        log.info {
            "Deleted unexpected ids $unexpectedIds from storage ${this::class.simpleName} since associated ${E::class.simpleName} don't exist"
        }
    }
    .defaultIfEmpty(Unit)

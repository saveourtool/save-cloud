package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.ExecutionRepository
import com.saveourtool.save.backend.utils.collectAsJsonTo
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.deleteUnexpectedKeys
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.upload

import com.fasterxml.jackson.databind.ObjectMapper
import com.saveourtool.save.storage.AbstractSimpleStorageUsingProjectReactor
import com.saveourtool.save.utils.getLogger
import org.slf4j.Logger
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * A storage for storing additional data (ExecutionInfo) associated with test results
 */
@Service
class ExecutionInfoStorage(
    configProperties: ConfigProperties,
    private val s3Operations: S3Operations,
    private val objectMapper: ObjectMapper,
    private val executionRepository: ExecutionRepository,
) : AbstractSimpleStorageUsingProjectReactor<Long>(
    s3Operations,
    concatS3Key(configProperties.s3Storage.prefix, "executionInfo"),
) {
    /**
     * Init method to delete unexpected ids which are not associated to [com.saveourtool.save.entities.Execution]
     */
    override fun doInit(): Mono<Unit> = Mono.fromFuture {
        s3Operations.deleteUnexpectedKeys(
            storageName = "${this::class.simpleName}",
            commonPrefix = s3KeyManager.commonPrefix,
        ) { s3Key ->
            executionRepository.findById(s3Key.removePrefix(s3KeyManager.commonPrefix).toLong()).isEmpty
        }
    }.publishOn(s3Operations.scheduler)

    /**
     * Update ExecutionInfo if it's required ([ExecutionUpdateDto.failReason] not null)
     *
     * @param executionInfo
     * @return empty Mono
     */
    fun upsertIfRequired(executionInfo: ExecutionUpdateDto): Mono<Unit> = executionInfo.failReason?.let {
        upsert(executionInfo)
    } ?: Mono.just(Unit)

    private fun upsert(executionInfo: ExecutionUpdateDto): Mono<Unit> = doesExist(executionInfo.id)
        .flatMap { exists ->
            if (exists) {
                download(executionInfo.id)
                    .collectAsJsonTo<ExecutionUpdateDto>(objectMapper)
                    .map {
                        it.copy(failReason = "${it.failReason}, ${executionInfo.failReason}")
                    }
                    .flatMap { executionInfoToSafe ->
                        delete(executionInfo.id).map { executionInfoToSafe }
                    }
            } else {
                Mono.just(executionInfo)
            }
        }
        .flatMap { executionInfoToSafe ->
            log.debug { "Writing debug info for ${executionInfoToSafe.id} to storage" }
            upload(executionInfoToSafe.id, objectMapper.writeValueAsBytes(executionInfoToSafe))
        }
        .map { bytesCount ->
            log.debug { "Wrote $bytesCount bytes of debug info for ${executionInfo.id} to storage" }
        }

    override fun doBuildKeyFromSuffix(s3KeySuffix: String): Long = s3KeySuffix.toLong()
    override fun doBuildS3KeySuffix(key: Long): String = key.toString()

    companion object {
        private val log: Logger = getLogger<ExecutionInfoStorage>()
    }
}

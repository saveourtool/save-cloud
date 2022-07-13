package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.utils.toFluxByteBufferAsJson
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.storage.AbstractFileBasedStorage
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import org.slf4j.Logger
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

import java.io.InputStream
import java.io.SequenceInputStream
import java.nio.ByteBuffer
import java.nio.file.Path

import kotlin.io.path.div
import kotlin.io.path.name

/**
 * A storage for storing additional data (ExecutionInfo) associated with test results
 */
@Service
class ExecutionInfoStorage(
    configProperties: ConfigProperties,
    private val objectMapper: ObjectMapper,
) : AbstractFileBasedStorage<Long>(Path.of(configProperties.fileStorage.location) / "debugInfo") {
    /**
     * @param rootDir
     * @param pathToContent
     * @return true if filename is [FILE_NAME]
     */
    override fun isKey(rootDir: Path, pathToContent: Path): Boolean = pathToContent.name == FILE_NAME

    /**
     * @param rootDir
     * @param pathToContent
     * @return executionId from parent name
     */
    override fun buildKey(rootDir: Path, pathToContent: Path): Long = pathToContent.parent.name.toLong()

    /**
     * @param rootDir
     * @param key
     * @return [Path] to content
     */
    override fun buildPathToContent(rootDir: Path, key: Long): Path = rootDir / key.toString() / FILE_NAME

    /**
     * @param executionInfo
     */
    fun upsert(executionInfo: ExecutionUpdateDto) {
        doesExist(executionInfo.id)
            .flatMap { exists ->
                if (exists) {
                    download(executionInfo.id)
                        .collectToInputStream()
                        .map {
                            readExecutionInfo(it)
                        }
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
                upload(executionInfoToSafe.id, executionInfoToSafe.toFluxByteBufferAsJson(objectMapper))
            }
            .subscribeOn(Schedulers.immediate())
            .toFuture()
            .get()
    }

    private fun Flux<ByteBuffer>.collectToInputStream(): Mono<InputStream> = this
        .map {
            // take simple implementation from Jackson library
            ByteBufferBackedInputStream(it)
        }
        .cast(InputStream::class.java)
        .reduce { in1, in2 ->
            SequenceInputStream(in1, in2)
        }

    private fun readExecutionInfo(inputStream: InputStream): ExecutionUpdateDto = objectMapper.readValue(inputStream, ExecutionUpdateDto::class.java)

    companion object {
        private val log: Logger = getLogger<ExecutionInfoStorage>()
        private const val FILE_NAME = "execution-info.json"
    }
}

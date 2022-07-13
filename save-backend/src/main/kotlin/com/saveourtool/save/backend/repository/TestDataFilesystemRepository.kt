package com.saveourtool.save.backend.repository

import com.saveourtool.save.backend.storage.DebugInfoStorage
import com.saveourtool.save.backend.storage.ExecutionInfoStorage
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.domain.TestResultLocation
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.utils.debug

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toFlux

import java.io.InputStream
import java.io.SequenceInputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * A repository for storing additional data associated with test results
 */
@Repository
class TestDataFilesystemRepository(
    private val debugInfoStorage: DebugInfoStorage,
    private val executionInfoStorage: ExecutionInfoStorage,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(TestDataFilesystemRepository::class.java)

    /**
     * Store provided [testResultDebugInfo] associated with [executionId]
     *
     * @param executionId
     * @param testResultDebugInfo
     */
    fun save(
        executionId: Long,
        testResultDebugInfo: TestResultDebugInfo,
    ) {
        with(testResultDebugInfo) {
            log.debug { "Writing debug info for $executionId to $testResultLocation" }
            debugInfoStorage.upload(Pair(executionId, testResultLocation), testResultDebugInfo.toFluxByteBufferAsJson())
                .subscribeOn(Schedulers.immediate())
                .toFuture()
                .get()
        }
    }

    /**
     * @param executionId
     * @param testResultLocation
     * @return content of additional data for provided [executionId] and [testResultLocation]
     */
    fun getDebugInfoContent(
        executionId: Long,
        testResultLocation: TestResultLocation,
    ): String = debugInfoStorage.download(Pair(executionId, testResultLocation))
        .collectToString()
        .subscribeOn(Schedulers.immediate())
        .toFuture()
        .get()

    /**
     * @param executionId
     * @return content of additional data for provided [executionId]
     */
    fun getExecutionInfoContent(
        executionId: Long,
    ): String = executionInfoStorage.download(executionId)
        .collectToString()
        .subscribeOn(Schedulers.immediate())
        .toFuture()
        .get()

    /**
     * @param executionId
     * @param testResultLocation
     * @return true if file with additional data exists, otherwise - false
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun doesDebugInfoExist(executionId: Long, testResultLocation: TestResultLocation): Boolean =
            debugInfoStorage.doesExist(Pair(executionId, testResultLocation))
                .subscribeOn(Schedulers.immediate())
                .toFuture()
                .get()

    /**
     * @param executionId
     * @return true if file with additional data exists, otherwise - false
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun doesExecutionInfoExist(executionId: Long): Boolean =
            executionInfoStorage.doesExist(executionId)
                .subscribeOn(Schedulers.immediate())
                .toFuture()
                .get()

    /**
     * @param executionInfo
     */
    fun save(executionInfo: ExecutionUpdateDto) {
        executionInfoStorage.doesExist(executionInfo.id)
            .flatMap { exists ->
                if (exists) {
                    executionInfoStorage.download(executionInfo.id)
                        .collectToInputStream()
                        .map {
                            readExecutionInfo(it)
                        }
                        .map {
                            it.copy(failReason = "${it.failReason}, ${executionInfo.failReason}")
                        }
                } else {
                    Mono.just(executionInfo)
                }
            }
            .flatMap { executionInfoToSafe ->
                log.debug { "Writing debug info for ${executionInfoToSafe.id} to storage" }
                executionInfoStorage.upload(executionInfoToSafe.id, executionInfoToSafe.toFluxByteBufferAsJson())
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

    private fun Flux<ByteBuffer>.collectToString(): Mono<String> = this
        .collectToInputStream()
        .map {
            IOUtils.toString(it, StandardCharsets.UTF_8)
        }

    private fun <T> T.toFluxByteBufferAsJson(): Flux<ByteBuffer> = Mono.fromCallable { objectMapper.writeValueAsBytes(this) }
        .map { ByteBuffer.wrap(it) }
        .toFlux()

    private fun readExecutionInfo(inputStream: InputStream): ExecutionUpdateDto = objectMapper.readValue(inputStream, ExecutionUpdateDto::class.java)
}

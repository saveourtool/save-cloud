package com.saveourtool.save.backend.repository

import com.saveourtool.save.backend.storage.DebugInfoStorage
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.domain.TestResultLocation
import com.saveourtool.save.utils.debug

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
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
    private val objectMapper: ObjectMapper,
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
            val content = Mono.fromCallable { objectMapper.writeValueAsBytes(testResultDebugInfo) }
                .map { ByteBuffer.wrap(it) }
                .toFlux()
            log.debug { "Writing debug info for $executionId to $testResultLocation" }
            debugInfoStorage.upload(Pair(executionId, testResultLocation), content)
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
    fun getContent(
        executionId: Long,
        testResultLocation: TestResultLocation,
    ): String = debugInfoStorage.download(Pair(executionId, testResultLocation))
        // take simple implementation from Jackson library
        .map { ByteBufferBackedInputStream(it) }
        .cast(InputStream::class.java)
        .reduce { in1, in2 -> SequenceInputStream(in1, in2) }
        .map {
            IOUtils.toString(it, StandardCharsets.UTF_8)
        }
        .subscribeOn(Schedulers.immediate())
        .toFuture()
        .get()

    /**
     * @param executionId
     * @param testResultLocation
     * @return true if file with additional data exists, otherwise - false
     */
    fun doesExist(executionId: Long, testResultLocation: TestResultLocation): Boolean =
            debugInfoStorage.doesExist(Pair(executionId, testResultLocation))
                .subscribeOn(Schedulers.immediate())
                .toFuture()
                .get()
}

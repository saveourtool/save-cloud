package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.repository.TestSuitesSourceSnapshotRepository
import com.saveourtool.save.backend.repository.TestSuitesSourceVersionRepository
import com.saveourtool.save.storage.Storage
import com.saveourtool.save.testsuite.TestSuitesSourceVersionDto
import com.saveourtool.save.utils.*
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.nio.ByteBuffer
import java.time.Instant
import kotlin.io.path.*

/**
 * Wrapper for [NewTestSuitesSourceSnapshotStorage] using [TestSuitesSourceVersionDto] as a key
 */
@Service
class TestSuitesSourceVersionStorage(
    private val versionRepository: TestSuitesSourceVersionRepository,
    private val snapshotRepository: TestSuitesSourceSnapshotRepository,
    private val snapshotStorage: NewTestSuitesSourceSnapshotStorage,
) : Storage<TestSuitesSourceVersionDto> {
    override fun list(): Flux<TestSuitesSourceVersionDto> = blockingToFlux {
        versionRepository.findAll().map { it.toDto() }
    }

    override fun doesExist(key: TestSuitesSourceVersionDto): Mono<Boolean> = key.toEntityAsMono()
        .flatMap { entity ->
            snapshotStorage.doesExist(entity.snapshot.toDto())
                .filter { it }
                .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
                    "The version $key is presented in database, but missed in snapshot storage"
                }
        }
        .defaultIfEmpty(false)

    override fun download(key: TestSuitesSourceVersionDto): Flux<ByteBuffer> = key.toEntityAsMono()
        .flatMapMany { snapshotStorage.download(it.snapshot.toDto()) }

    override fun upload(key: TestSuitesSourceVersionDto, content: Flux<ByteBuffer>): Mono<Long> = key.toEntityAsMono()
        .map { it.snapshot.toDto() }
        .flatMap { snapshot ->
            snapshotStorage.doesExist(snapshot)
                .filter { !it }
                .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
                    "The snapshot ${key.snapshotId} (for version ${key.name}) is presented in storage already"
                }
                .map { snapshot }
        }
        .flatMap { snapshotStorage.upload(it, content) }

    override fun delete(key: TestSuitesSourceVersionDto): Mono<Boolean> = blockingToMono {
        findEntity(key)
            ?.let { entity ->
                val snapshot = entity.snapshot
                versionRepository.delete(entity)
                if (versionRepository.findAllBySnapshot(snapshot).isEmpty()) {
                    snapshotStorage.delete(snapshot.toDto())
                }
                true
            }
            ?: false
    }

    override fun lastModified(key: TestSuitesSourceVersionDto): Mono<Instant> = key.toEntityAsMono()
        .flatMap { snapshotStorage.lastModified(it.snapshot.toDto()) }

    override fun contentSize(key: TestSuitesSourceVersionDto): Mono<Long> = key.toEntityAsMono()
        .flatMap { snapshotStorage.contentSize(it.snapshot.toDto()) }

    private fun findEntity(dto: TestSuitesSourceVersionDto) = dto.id
        ?.let {
            versionRepository.getByIdOrNotFound(it)
        }
        ?.let {
            snapshotRepository.findByIdOrNull(dto.snapshotId)
                ?.let {
                    versionRepository.findBySnapshotAndName(it, dto.name)
                }
        }

    private fun TestSuitesSourceVersionDto.toEntityAsMono() = blockingToMono {
        findEntity(this)
    }
}

package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.TestsSourceSnapshotRepository
import com.saveourtool.save.backend.repository.TestsSourceVersionRepository
import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.backend.service.TestsSourceVersionService
import com.saveourtool.save.entities.TestsSourceSnapshot
import com.saveourtool.save.entities.TestsSourceVersion
import com.saveourtool.save.request.TestFilesRequest
import com.saveourtool.save.storage.Storage
import com.saveourtool.save.test.*
import com.saveourtool.save.test.TestFilesContent
import com.saveourtool.save.test.TestFilesRequest
import com.saveourtool.save.test.TestsSourceVersionInfo
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.*

import org.springframework.context.annotation.Lazy
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.nio.ByteBuffer
import java.time.Instant

import kotlin.io.path.*

/**
 * Service for [TestsSourceVersionInfo]
 */
@Service
class TestsSourceVersionInfoStorage(
    @Lazy
    private val snapshotStorage: MigrationTestsSourceSnapshotStorage,
    @Lazy
    private val testsSourceVersionService: TestsSourceVersionService,
    private val testsSourceSnapshotRepository: TestsSourceSnapshotRepository,
    private val testsSourceVersionRepository: TestsSourceVersionRepository,
    private val userRepository: UserRepository,
    configProperties: ConfigProperties,
) : Storage<TestsSourceVersionInfo> {
    /**
     * @param organizationName
     * @param sourceName
     * @param version
     * @return true if storage contains snapshot with provided values, otherwise -- false
     */
    fun doesContain(
        organizationName: String,
        sourceName: String,
        version: String,
    ): Mono<Boolean> = snapshotStorage.findSnapshotKey(organizationName, sourceName, version)
        .map { true }
        .defaultIfEmpty(false)

    /**
     * @param organizationName
     * @param sourceName
     * @param version
     * @return content of a key which contains provided values
     */
    fun download(
        organizationName: String,
        sourceName: String,
        version: String,
    ): Flux<ByteBuffer> = snapshotStorage.findSnapshotKey(
        organizationName = organizationName,
        sourceName = sourceName,
        version = version,
    ).flatMapMany { snapshotStorage.download(it) }

    /**
     * @param organizationName
     * @param sourceName
     * @param version
     * @return result of deletion of a key which contains provided values
     */
    @Transactional
    fun delete(
        organizationName: String,
        sourceName: String,
        version: String,
    ): Mono<Boolean> = blockingToMono { doDeleteVersion(organizationName, sourceName, version) }
        .filter { it }
        .then(
            snapshotStorage.findSnapshotKey(
                organizationName = organizationName,
                sourceName = sourceName,
                version = version,
            )
        )
        .flatMap { snapshotStorage.delete(it) }
        .defaultIfEmpty(true)

    /**
     * @param organizationName
     * @param sourceName
     * @param version
     * @return true if [TestsSourceSnapshot] related to deleted [TestsSourceVersion] doesn't have another [TestsSourceVersion] related to it
     */
    private fun doDeleteVersion(
        organizationName: String,
        sourceName: String,
        version: String,
    ): Boolean {
        val versionEntity =
            testsSourceVersionRepository.findBySnapshot_Source_Organization_NameAndSnapshot_Source_NameAndName(
                organizationName = organizationName,
                sourceName = sourceName,
                version = version,
            ).orNotFound {
                "Not found ${TestsSourceVersion::class.simpleName} with version $version in $organizationName/$sourceName"
            }
        testsSourceVersionRepository.delete(versionEntity)
        val snapshot = versionEntity.snapshot
        return testsSourceVersionRepository.findAllBySnapshot(snapshot).isEmpty()
    }

    /**
     * @param organizationName
     * @return list of [TestsSourceVersionInfo] found by provided values
     */
    fun list(
        organizationName: String,
    ): Flux<TestsSourceVersionInfo> {
        require(snapshotStorage.isMigrationFinished())
        return blockingToFlux {
            testsSourceVersionService.getAllAsInfo(organizationName)
        }
    }

    /**
     * @param organizationName
     * @param sourceName
     * @return list of [TestsSourceVersionInfo] found by provided values
     */
    fun list(
        organizationName: String,
        sourceName: String,
    ): Flux<TestsSourceVersionInfo> {
        require(snapshotStorage.isMigrationFinished())
        return blockingToFlux {
            testsSourceVersionService.getAllAsInfo(organizationName, sourceName)
        }
    }

    override fun list(): Flux<TestsSourceVersionInfo> {
        require(snapshotStorage.isMigrationFinished())
        return blockingToFlux {
            testsSourceVersionService.getAllAsInfo()
        }
    }

    override fun download(key: TestsSourceVersionInfo): Flux<ByteBuffer> = snapshotStorage.download(key.toKey())

    override fun upload(key: TestsSourceVersionInfo, content: Flux<ByteBuffer>): Mono<Long> {
        TODO("Not yet implemented")
    }

    override fun delete(key: TestsSourceVersionInfo): Mono<Boolean> = blockingToMono {
        testsSourceVersionService.delete(key.organizationName, key.sourceName, key.version)
    }
        .filter { it }
        .flatMap {
            snapshotStorage.delete(key.toKey())
        }
        .defaultIfEmpty(true)

    override fun lastModified(key: TestsSourceVersionInfo): Mono<Instant> = snapshotStorage.lastModified(key.toKey())

    override fun contentSize(key: TestsSourceVersionInfo): Mono<Long> = snapshotStorage.contentSize(key.toKey())

    override fun doesExist(key: TestsSourceVersionInfo): Mono<Boolean> = blockingToMono {
        testsSourceVersionService.findByInfo(key) != null
    }

    companion object {
        private fun TestsSourceVersionInfo.toKey() = TestSuitesSourceSnapshotKey(
            organizationName = organizationName,
            testSuitesSourceName = sourceName,
            version = commitId,
            creationTime = commitTime,
        )
    }
}
